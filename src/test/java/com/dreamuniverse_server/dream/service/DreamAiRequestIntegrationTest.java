package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import com.dreamuniverse_server.ad.entity.RewardedAdStatus;
import com.dreamuniverse_server.ad.repository.RewardedAdSessionRepository;
import com.dreamuniverse_server.ad.service.RewardedAdSessionService;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import com.dreamuniverse_server.dream.policy.DreamUsagePolicy;
import com.dreamuniverse_server.dream.repository.DreamAiUsageRepository;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        DreamAiRequestService.class,
        DreamAiUsageService.class,
        RewardedAdSessionService.class
})
class DreamAiRequestIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("dreamuniverse_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
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
    private DreamAiRequestService requestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DreamRepository dreamRepository;

    @Autowired
    private DreamAiUsageRepository usageRepository;

    @Autowired
    private RewardedAdSessionRepository adRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private DreamEntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        when(entitlementService.getPolicy(anyLong()))
                .thenReturn(new DreamUsagePolicy(3, 4, true));
    }

    @Test
    void 정상_요청은_사용량과_광고를_소비하고_PROCESSING으로_전환한다() {
        Fixture fixture = createFixture();

        LocalDate usageDate = requestService.prepare(
                fixture.user().getId(),
                fixture.user(),
                fixture.dream1(),
                fixture.session1()
        );

        assertEquals(1, usedCount(
                fixture.user().getId(),
                usageDate
        ));

        assertEquals(
                RewardedAdStatus.CONSUMED,
                adStatus(fixture.session1())
        );

        assertEquals(
                DreamAiStatus.PROCESSING,
                dreamStatus(fixture.dream1())
        );
    }

    @Test
    void 광고_보상이_만료되면_사용량_증가가_롤백된다() {
        Fixture fixture = createFixture();

        // 이미 보상 사용 기간이 지난 별도 세션 생성
        String expiredSessionId = createExpiredSession(
                fixture.user().getId()
        );

        assertThrows(
                CustomException.class,
                () -> requestService.prepare(
                        fixture.user().getId(),
                        fixture.user(),
                        fixture.dream1(),
                        expiredSessionId
                )
        );

        LocalDate today = LocalDate.now(
                java.time.ZoneId.of("Asia/Seoul")
        );

        assertEquals(
                0,
                usedCount(fixture.user().getId(), today)
        );

        assertEquals(
                RewardedAdStatus.VERIFIED,
                adStatus(expiredSessionId)
        );

        assertEquals(
                DreamAiStatus.NOT_REQUESTED,
                dreamStatus(fixture.dream1())
        );
    }

    @Test
    void 하루_3회를_모두_사용하면_광고를_소비하지_않는다() {
        Fixture fixture = createFixture();

        LocalDate today = LocalDate.now(
                java.time.ZoneId.of("Asia/Seoul")
        );

        initializeUsage(
                fixture.user().getId(),
                today,
                3
        );

        assertThrows(
                CustomException.class,
                () -> requestService.prepare(
                        fixture.user().getId(),
                        fixture.user(),
                        fixture.dream1(),
                        fixture.session1()
                )
        );

        assertEquals(
                3,
                usedCount(fixture.user().getId(), today)
        );

        assertEquals(
                RewardedAdStatus.VERIFIED,
                adStatus(fixture.session1())
        );

        assertEquals(
                DreamAiStatus.NOT_REQUESTED,
                dreamStatus(fixture.dream1())
        );
    }

    @Test
    void 동일_광고_세션으로_동시_요청하면_한_번만_성공한다()
            throws Exception {

        Fixture fixture = createFixture();

        // 서로 다른 꿈에 동일한 광고 세션을 제출
        int successes = runConcurrent(
                () -> requestService.prepare(
                        fixture.user().getId(),
                        fixture.user(),
                        fixture.dream1(),
                        fixture.session1()
                ),
                () -> requestService.prepare(
                        fixture.user().getId(),
                        fixture.user(),
                        fixture.dream2(),
                        fixture.session1()
                )
        );

        assertEquals(1, successes);

        LocalDate today = LocalDate.now(
                java.time.ZoneId.of("Asia/Seoul")
        );

        assertEquals(
                1,
                usedCount(fixture.user().getId(), today)
        );

        assertEquals(
                RewardedAdStatus.CONSUMED,
                adStatus(fixture.session1())
        );

        assertEquals(
                1,
                countProcessingDreams(fixture)
        );
    }

    @Test
    void 잔여_사용량이_1회라면_서로_다른_동시_요청도_하나만_성공한다()
            throws Exception {

        Fixture fixture = createFixture();

        LocalDate today = LocalDate.now(
                java.time.ZoneId.of("Asia/Seoul")
        );

        initializeUsage(
                fixture.user().getId(),
                today,
                2
        );

        int successes = runConcurrent(
                () -> requestService.prepare(
                        fixture.user().getId(),
                        fixture.user(),
                        fixture.dream1(),
                        fixture.session1()
                ),
                () -> requestService.prepare(
                        fixture.user().getId(),
                        fixture.user(),
                        fixture.dream2(),
                        fixture.session2()
                )
        );

        assertEquals(1, successes);

        assertEquals(
                3,
                usedCount(fixture.user().getId(), today)
        );

        assertEquals(
                1,
                countProcessingDreams(fixture)
        );

        int consumedCount = 0;

        if (adStatus(fixture.session1())
                == RewardedAdStatus.CONSUMED) {
            consumedCount++;
        }

        if (adStatus(fixture.session2())
                == RewardedAdStatus.CONSUMED) {
            consumedCount++;
        }

        assertEquals(1, consumedCount);
    }

    // --------------------------------------------------
    // 테스트 데이터 생성
    // --------------------------------------------------

    private Fixture createFixture() {
        return tx().execute(status -> {

            String identifier =
                    UUID.randomUUID().toString();

            User user = User.create(
                    OAuthProvider.GOOGLE,
                    "test-" + identifier
            );

            user.completeSignup(
                    identifier + "@example.com",
                    "Test User",
                    "tester",
                    LocalDate.of(2000, 1, 1),
                    "Asia/Seoul",
                    Instant.now()
            );

            userRepository.saveAndFlush(user);

            Dream dream1 = Dream.create(
                    user,
                    "테스트 꿈 1",
                    "첫 번째 꿈 내용",
                    LocalDate.now(),
                    false,
                    false,
                    false
            );

            Dream dream2 = Dream.create(
                    user,
                    "테스트 꿈 2",
                    "두 번째 꿈 내용",
                    LocalDate.now(),
                    false,
                    false,
                    false
            );

            dreamRepository.saveAndFlush(dream1);
            dreamRepository.saveAndFlush(dream2);

            String session1 =
                    createVerifiedSessionInTx(user.getId());

            String session2 =
                    createVerifiedSessionInTx(user.getId());

            return new Fixture(
                    user,
                    dream1.getId(),
                    dream2.getId(),
                    session1,
                    session2
            );
        });
    }

    private String createVerifiedSessionInTx(
            Long userId
    ) {
        Instant now = Instant.now();

        RewardedAdSession session =
                RewardedAdSession.create(
                        userId,
                        now.minusSeconds(10)
                );

        session.verify(
                UUID.randomUUID().toString(),
                now
        );

        adRepository.saveAndFlush(session);

        return session.getId();
    }

    private String createExpiredSession(Long userId) {
        return tx().execute(status -> {

            Instant now = Instant.now();

            RewardedAdSession session =
                    RewardedAdSession.create(
                            userId,
                            now.minusSeconds(12 * 60)
                    );

            session.verify(
                    UUID.randomUUID().toString(),
                    now.minusSeconds(11 * 60)
            );

            adRepository.saveAndFlush(session);

            return session.getId();
        });
    }

    private void initializeUsage(
            Long userId,
            LocalDate date,
            int count
    ) {
        tx().executeWithoutResult(status -> {
            usageRepository.insertIfAbsent(
                    userId,
                    date
            );

            for (int i = 0; i < count; i++) {
                int updated =
                        usageRepository.incrementIfAvailable(
                                userId,
                                date,
                                3
                        );

                assertEquals(1, updated);
            }
        });
    }

    // --------------------------------------------------
    // DB 상태 조회
    // --------------------------------------------------

    private int usedCount(
            Long userId,
            LocalDate date
    ) {
        return usageRepository
                .findByUserIdAndUsageDate(userId, date)
                .map(DreamAiUsage::getUsedCount)
                .orElse(0);
    }

    private RewardedAdStatus adStatus(
            String sessionId
    ) {
        return adRepository.findById(sessionId)
                .orElseThrow()
                .getStatus();
    }

    private DreamAiStatus dreamStatus(
            Long dreamId
    ) {
        return dreamRepository.findById(dreamId)
                .orElseThrow()
                .getAiStatus();
    }

    private int countProcessingDreams(
            Fixture fixture
    ) {
        int count = 0;

        if (dreamStatus(fixture.dream1())
                == DreamAiStatus.PROCESSING) {
            count++;
        }

        if (dreamStatus(fixture.dream2())
                == DreamAiStatus.PROCESSING) {
            count++;
        }

        return count;
    }

    // --------------------------------------------------
    // 동시성 실행
    // --------------------------------------------------

    private int runConcurrent(
            Callable<LocalDate> first,
            Callable<LocalDate> second
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start =
                new CountDownLatch(1);

        try {
            List<Future<Boolean>> futures =
                    new ArrayList<>();

            for (Callable<LocalDate> task :
                    List.of(first, second)) {

                futures.add(executor.submit(() -> {
                    start.await();

                    try {
                        task.call();
                        return true;
                    } catch (CustomException expected) {
                        return false;
                    }
                }));
            }

            start.countDown();

            int successes = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successes++;
                }
            }

            return successes;

        } finally {
            executor.shutdownNow();
        }
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(
                transactionManager
        );
    }

    private record Fixture(
            User user,
            Long dream1,
            Long dream2,
            String session1,
            String session2
    ) {
    }
}
