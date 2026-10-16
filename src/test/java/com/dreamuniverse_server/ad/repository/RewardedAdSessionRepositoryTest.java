package com.dreamuniverse_server.ad.repository;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import com.dreamuniverse_server.ad.entity.RewardedAdStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false"
})
@AutoConfigureTestDatabase(replace =
        AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RewardedAdSessionRepositoryIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("dreamuniverse_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                mysql::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                mysql::getUsername
        );
        registry.add(
                "spring.datasource.password",
                mysql::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                mysql::getDriverClassName
        );
    }

    @Autowired
    private RewardedAdSessionRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 동일한_광고_보상은_동시에_소비해도_한_번만_성공한다()
            throws Exception {

        String sessionId = createVerifiedSession("transaction-A");

        int concurrentRequests = 2;

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrentRequests);

        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            List<Future<Boolean>> results = new ArrayList<>();

            for (int i = 0; i < concurrentRequests; i++) {
                results.add(executor.submit(() -> {
                    startSignal.await();

                    TransactionTemplate tx =
                            new TransactionTemplate(transactionManager);

                    return Boolean.TRUE.equals(tx.execute(status -> {
                        RewardedAdSession session = repository
                                .findByIdAndUserIdForUpdate(
                                        sessionId,
                                        1L
                                )
                                .orElseThrow();

                        if (session.getStatus()
                                != RewardedAdStatus.VERIFIED) {
                            return false;
                        }

                        session.consume(Instant.now());

                        return true;
                    }));
                }));
            }

            startSignal.countDown();

            int successCount = 0;

            for (Future<Boolean> future : results) {
                if (future.get(15, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            assertEquals(1, successCount);

            RewardedAdSession saved =
                    repository.findById(sessionId).orElseThrow();

            assertEquals(
                    RewardedAdStatus.CONSUMED,
                    saved.getStatus()
            );

            assertNotNull(saved.getConsumedAt());

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 동일한_transaction_id는_서로_다른_세션에_저장할_수_없다() {

        String transactionId = "same-transaction-id";

        createVerifiedSession(transactionId);

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> tx.executeWithoutResult(status -> {
                    RewardedAdSession another =
                            RewardedAdSession.create(
                                    2L,
                                    Instant.now()
                            );

                    another.verify(
                            transactionId,
                            Instant.now()
                    );

                    repository.saveAndFlush(another);
                })
        );
    }

    @Test
    void 트랜잭션_롤백시_소비된_광고_보상이_복구된다() {

        String sessionId =
                createVerifiedSession("rollback-transaction");

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        assertThrows(
                IllegalStateException.class,
                () -> tx.executeWithoutResult(status -> {
                    RewardedAdSession session = repository
                            .findByIdAndUserIdForUpdate(
                                    sessionId,
                                    1L
                            )
                            .orElseThrow();

                    session.consume(Instant.now());

                    repository.flush();

                    throw new IllegalStateException(
                            "Simulated transaction failure"
                    );
                })
        );

        RewardedAdSession restored =
                repository.findById(sessionId).orElseThrow();

        assertEquals(
                RewardedAdStatus.VERIFIED,
                restored.getStatus()
        );

        assertNull(restored.getConsumedAt());
    }

    private String createVerifiedSession(String transactionId) {

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        return tx.execute(status -> {
            Instant now = Instant.now();

            RewardedAdSession session =
                    RewardedAdSession.create(
                            1L,
                            now.minusSeconds(5)
                    );

            session.verify(transactionId, now);

            repository.saveAndFlush(session);

            return session.getId();
        });
    }
}
