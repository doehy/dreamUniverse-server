package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import com.dreamuniverse_server.dream.repository.DreamAiUsageRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest
class DreamAiUsageServiceTest {

    @Container
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("dreamuniverse_test")
                    .withUsername("test")
                    .withPassword("test");


    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        /*
         * Spring Boot가 실제 개발 DB가 아니라
         * Testcontainers가 띄운 MySQL에 연결하도록 한다.
         */
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

        /*
         * 테스트 컨테이너 안에 테스트용 테이블을 생성하고
         * 테스트가 끝나면 제거한다.
         */
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-only"
        );

        /*
         * 이 테스트에서는 실제 OpenAI API를 호출하지 않는다.
         *
         * 하지만 @SpringBootTest는 전체 Spring Context를 띄우기 때문에
         * OpenAiConfig가 API Key를 요구할 수 있다.
         *
         * 따라서 테스트 Context 생성용 가짜 값을 넣어준다.
         */
        registry.add(
                "openai.api-key",
                () -> "test-api-key"
        );
    }


    @Autowired
    private DreamAiUsageService dreamAiUsageService;

    @Autowired
    private DreamAiUsageRepository dreamAiUsageRepository;

    @Autowired
    private UserRepository userRepository;


    private User user;


    @BeforeEach
    void setUp() {

        user = User.create(
                OAuthProvider.GOOGLE,
                "dream-ai-usage-test-user"
        );

        user.completeSignup(
                "dream-ai-usage@test.com",
                "Test User",
                "tester",
                LocalDate.of(2000, 1, 1),
                "Asia/Seoul",
                Instant.now()
        );

        user = userRepository.saveAndFlush(user);
    }


    @AfterEach
    void tearDown() {

        /*
         * DreamAiUsage → User FK가 있으므로
         * 자식 테이블부터 삭제해야 한다.
         */
        dreamAiUsageRepository.deleteAll();
        userRepository.deleteAll();
    }


    @Test
    void 첫_AI_사용시_사용량이_1_증가한다() {

        // when
        LocalDate usageDate =
                dreamAiUsageService.acquire(user);

        // then
        DreamAiUsage usage =
                dreamAiUsageRepository
                        .findByUserIdAndUsageDate(
                                user.getId(),
                                usageDate
                        )
                        .orElseThrow();

        assertThat(usage.getUsedCount())
                .isEqualTo(1);
    }


    @Test
    void 무료_사용자는_하루에_2번까지_AI를_사용할_수_있다() {

        // when
        LocalDate firstUsageDate =
                dreamAiUsageService.acquire(user);

        LocalDate secondUsageDate =
                dreamAiUsageService.acquire(user);

        // then
        assertThat(secondUsageDate)
                .isEqualTo(firstUsageDate);

        DreamAiUsage usage =
                dreamAiUsageRepository
                        .findByUserIdAndUsageDate(
                                user.getId(),
                                firstUsageDate
                        )
                        .orElseThrow();

        assertThat(usage.getUsedCount())
                .isEqualTo(2);
    }


    @Test
    void 무료_일일_사용량_2회를_초과하면_예외가_발생한다() {

        // given
        dreamAiUsageService.acquire(user);
        dreamAiUsageService.acquire(user);

        // when & then
        assertThatThrownBy(
                () -> dreamAiUsageService.acquire(user)
        )
                .isInstanceOfSatisfying(
                        CustomException.class,
                        e -> assertThat(e.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.DREAM_AI_DAILY_LIMIT_EXCEEDED
                                )
                );

        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                user.getTimezone()
                        )
                );

        DreamAiUsage usage =
                dreamAiUsageRepository
                        .findByUserIdAndUsageDate(
                                user.getId(),
                                today
                        )
                        .orElseThrow();

        /*
         * 세 번째 요청이 실패했으므로
         * usedCount가 3이 되어서는 안 된다.
         */
        assertThat(usage.getUsedCount())
                .isEqualTo(2);
    }


    @Test
    void release하면_선점했던_AI_사용량이_1_감소한다() {

        // given
        LocalDate usageDate =
                dreamAiUsageService.acquire(user);

        // when
        dreamAiUsageService.release(
                user.getId(),
                usageDate
        );

        // then
        DreamAiUsage usage =
                dreamAiUsageRepository
                        .findByUserIdAndUsageDate(
                                user.getId(),
                                usageDate
                        )
                        .orElseThrow();

        assertThat(usage.getUsedCount())
                .isZero();
    }


    @Test
    void 동시에_3번_요청해도_2번만_사용권을_획득한다()
            throws Exception {

        // given
        int requestCount = 3;

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        requestCount
                );

        try {

            /*
             * 각 스레드가 출발 준비를 마쳤는지 확인하기 위한 latch.
             */
            CountDownLatch readyLatch =
                    new CountDownLatch(
                            requestCount
                    );

            /*
             * 모든 스레드를 동시에 출발시키기 위한 latch.
             */
            CountDownLatch startLatch =
                    new CountDownLatch(1);

            List<Future<Boolean>> futures =
                    new ArrayList<>();


            for (int i = 0; i < requestCount; i++) {

                futures.add(
                        executorService.submit(() -> {

                            /*
                             * 현재 스레드가 준비됐음을 알린다.
                             */
                            readyLatch.countDown();

                            /*
                             * 메인 테스트 스레드가 출발 신호를 줄 때까지 기다린다.
                             */
                            startLatch.await();

                            try {

                                dreamAiUsageService.acquire(
                                        user
                                );

                                return true;

                            } catch (CustomException e) {

                                /*
                                 * 일일 한도 초과는
                                 * 정상적으로 예상한 실패이다.
                                 */
                                return false;
                            }
                        })
                );
            }


            /*
             * 세 스레드가 모두 준비될 때까지 기다린다.
             */
            boolean allReady =
                    readyLatch.await(
                            5,
                            TimeUnit.SECONDS
                    );

            assertThat(allReady)
                    .isTrue();


            /*
             * 준비된 3개 스레드를 거의 동시에 출발시킨다.
             */
            startLatch.countDown();


            int successCount = 0;
            int failureCount = 0;

            for (Future<Boolean> future : futures) {

                if (future.get(
                        10,
                        TimeUnit.SECONDS
                )) {

                    successCount++;

                } else {

                    failureCount++;
                }
            }


            // then

            /*
             * 무료 사용자는 하루 최대 2회이므로
             * 세 요청 중 정확히 두 개만 성공해야 한다.
             */
            assertThat(successCount)
                    .isEqualTo(2);

            assertThat(failureCount)
                    .isEqualTo(1);


            LocalDate today =
                    LocalDate.now(
                            ZoneId.of(
                                    user.getTimezone()
                            )
                    );

            DreamAiUsage usage =
                    dreamAiUsageRepository
                            .findByUserIdAndUsageDate(
                                    user.getId(),
                                    today
                            )
                            .orElseThrow();


            /*
             * Java 레벨 성공 횟수뿐 아니라
             * 실제 DB의 최종 값도 정확히 2여야 한다.
             */
            assertThat(usage.getUsedCount())
                    .isEqualTo(2);

        } finally {

            /*
             * 테스트 중 예외가 발생하더라도
             * Executor의 스레드가 남지 않도록 반드시 종료한다.
             */
            executorService.shutdownNow();
        }
    }


    @Test
    void usageDate는_사용자의_timezone을_기준으로_계산한다() {

        // given

        /*
         * UTC+14
         */
        User kiribatiUser =
                createUser(
                        "timezone-kiribati",
                        "kiribati@test.com",
                        "kiribati",
                        "Pacific/Kiritimati"
                );

        /*
         * UTC-10
         *
         * Kiritimati와 Honolulu는 24시간 차이라
         * 테스트 실행 시각과 관계없이 현지 날짜가 다르다.
         */
        User hawaiiUser =
                createUser(
                        "timezone-hawaii",
                        "hawaii@test.com",
                        "hawaii",
                        "Pacific/Honolulu"
                );


        // when
        LocalDate kiribatiUsageDate =
                dreamAiUsageService.acquire(
                        kiribatiUser
                );

        LocalDate hawaiiUsageDate =
                dreamAiUsageService.acquire(
                        hawaiiUser
                );


        // then
        assertThat(kiribatiUsageDate)
                .isEqualTo(
                        LocalDate.now(
                                ZoneId.of(
                                        "Pacific/Kiritimati"
                                )
                        )
                );

        assertThat(hawaiiUsageDate)
                .isEqualTo(
                        LocalDate.now(
                                ZoneId.of(
                                        "Pacific/Honolulu"
                                )
                        )
                );

        /*
         * 사용자의 timezone에 따라
         * 같은 순간에도 usageDate가 달라질 수 있음을 확인한다.
         */
        assertThat(kiribatiUsageDate)
                .isNotEqualTo(
                        hawaiiUsageDate
                );
    }


    private User createUser(
            String providerUserId,
            String email,
            String nickname,
            String timezone
    ) {

        User newUser =
                User.create(
                        OAuthProvider.GOOGLE,
                        providerUserId
                );

        newUser.completeSignup(
                email,
                "Test User",
                nickname,
                LocalDate.of(2000, 1, 1),
                timezone,
                Instant.now()
        );

        return userRepository
                .saveAndFlush(
                        newUser
                );
    }
}