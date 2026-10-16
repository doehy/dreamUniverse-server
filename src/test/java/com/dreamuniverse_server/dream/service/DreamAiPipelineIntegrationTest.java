//package com.dreamuniverse_server.dream.service;
//
//import com.dreamuniverse_server.dream.entity.Dream;
//import com.dreamuniverse_server.dream.entity.DreamAiStatus;
//import com.dreamuniverse_server.dream.entity.DreamAiUsage;
//import com.dreamuniverse_server.dream.entity.DreamImage;
//import com.dreamuniverse_server.dream.repository.DreamAiUsageRepository;
//import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
//import com.dreamuniverse_server.dream.repository.DreamElementRepository;
//import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
//import com.dreamuniverse_server.dream.repository.DreamImageRepository;
//import com.dreamuniverse_server.dream.repository.DreamRepository;
//import com.dreamuniverse_server.global.config.R2Properties;
//import com.dreamuniverse_server.image.service.ImageStorageService;
//import com.dreamuniverse_server.user.entity.OAuthProvider;
//import com.dreamuniverse_server.user.entity.User;
//import com.dreamuniverse_server.user.repository.UserRepository;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
//import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.time.LocalDate;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.fail;
//
//@Testcontainers
//@SpringBootTest
//class DreamAiPipelineIntegrationTest {
//
//    @Container
//    static final MySQLContainer<?> mysql =
//            new MySQLContainer<>("mysql:8.4")
//                    .withDatabaseName("dreamuniverse_test")
//                    .withUsername("test")
//                    .withPassword("test");
//
//
//    @DynamicPropertySource
//    static void configureProperties(
//            DynamicPropertyRegistry registry
//    ) {
//        registry.add(
//                "spring.datasource.url",
//                mysql::getJdbcUrl
//        );
//
//        registry.add(
//                "spring.datasource.username",
//                mysql::getUsername
//        );
//
//        registry.add(
//                "spring.datasource.password",
//                mysql::getPassword
//        );
//
//        registry.add(
//                "spring.datasource.driver-class-name",
//                mysql::getDriverClassName
//        );
//
//        registry.add(
//                "spring.jpa.hibernate.ddl-auto",
//                () -> "create-only"
//        );
//    }
//
//
//    @Autowired
//    private DreamAiAsyncService dreamAiAsyncService;
//
//    @Autowired
//    private DreamAiUsageService dreamAiUsageService;
//
//    @Autowired
//    private DreamAiStatusService dreamAiStatusService;
//
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private DreamRepository dreamRepository;
//
//    @Autowired
//    private DreamAnalysisRepository dreamAnalysisRepository;
//
//    @Autowired
//    private DreamEmotionRepository dreamEmotionRepository;
//
//    @Autowired
//    private DreamElementRepository dreamElementRepository;
//
//    @Autowired
//    private DreamImageRepository dreamImageRepository;
//
//    @Autowired
//    private DreamAiUsageRepository dreamAiUsageRepository;
//
//
//    @Autowired
//    private S3Client r2S3Client;
//
//    @Autowired
//    private R2Properties r2Properties;
//
//    @Autowired
//    private ImageStorageService imageStorageService;
//
//
//    private User user;
//    private Dream dream;
//
//    private String generatedImageKey;
//
//
//    @BeforeEach
//    void setUp() {
//
//        /*
//         * 실제 DB에 가입 완료 User 생성
//         */
//        user = User.create(
//                OAuthProvider.GOOGLE,
//                "dream-ai-pipeline-test-user"
//        );
//
//        user.completeSignup(
//                "dream-ai-pipeline@test.com",
//                "Pipeline Test User",
//                "pipeline-tester",
//                LocalDate.of(
//                        2000,
//                        1,
//                        1
//                ),
//                "Asia/Seoul",
//                Instant.now()
//        );
//
//        user =
//                userRepository.saveAndFlush(
//                        user
//                );
//
//
//        /*
//         * 실제 DB에 Dream 생성
//         */
//        dream = Dream.create(
//                user,
//                "하늘을 나는 꿈",
//                """
//                밤에 집 밖으로 나왔는데 갑자기 몸이 공중으로 떠올랐다.
//                나는 도시 위를 천천히 날기 시작했다.
//                아래에는 건물들의 불빛이 반짝이고 있었다.
//                계속 앞으로 날아가자 아주 거대한 보름달이 나타났다.
//                나는 놀라서 달을 바라보았다.
//                """,
//                LocalDate.of(
//                        2026,
//                        9,
//                        23
//                )
//        );
//
//        dream =
//                dreamRepository.saveAndFlush(
//                        dream
//                );
//    }
//
//
//    @AfterEach
//    void tearDown() {
//
//        /*
//         * 테스트에서 실제 R2 파일이 생성됐다면 삭제
//         */
//        if (generatedImageKey != null) {
//            try {
//                imageStorageService.delete(
//                        generatedImageKey
//                );
//
//            } catch (RuntimeException e) {
//                System.err.println(
//                        "R2 cleanup failed: "
//                                + generatedImageKey
//                );
//            }
//        }
//
//
//        /*
//         * FK 순서 때문에 자식부터 삭제
//         */
//        dreamEmotionRepository.deleteAll();
//        dreamElementRepository.deleteAll();
//
//        dreamImageRepository.deleteAll();
//        dreamAnalysisRepository.deleteAll();
//
//        dreamAiUsageRepository.deleteAll();
//
//        dreamRepository.deleteAll();
//        userRepository.deleteAll();
//    }
//
//
//    @Test
//    void 실제_AI_비동기_파이프라인이_COMPLETED까지_완료된다()
//            throws Exception {
//
//        // given
//        Long userId =
//                user.getId();
//
//        Long dreamId =
//                dream.getId();
//
//        String content =
//                dream.getContent();
//
//
//        /*
//         * 실제 DreamService에서 AI 요청 시 하는 것과 동일하게
//         * 사용량을 먼저 확보한다.
//         */
//        LocalDate usageDate =
//                dreamAiUsageService.acquire(
//                        user
//                );
//
//
//        /*
//         * AI 작업 시작 상태
//         */
//        dreamAiStatusService.markProcessing(
//                dreamId
//        );
//
//
//        // when
//
//        /*
//         * 실제 Spring Bean을 호출한다.
//         *
//         * @Async("dreamAiExecutor")이 적용되므로
//         * 별도 Thread에서 실제 AI 작업이 실행된다.
//         */
//        dreamAiAsyncService.complete(
//                userId,
//                dreamId,
//                content,
//                usageDate
//        );
//
//
//        /*
//         * AI는 시간이 걸리므로
//         * COMPLETED 또는 FAILED가 될 때까지 기다린다.
//         *
//         * 최대 2분.
//         */
//        Dream finishedDream =
//                waitForAiCompletion(
//                        dreamId,
//                        Duration.ofMinutes(2)
//                );
//
//
//        // then
//
//        /*
//         * 최종 상태
//         */
//        assertThat(
//                finishedDream.getAiStatus()
//        ).isEqualTo(
//                DreamAiStatus.COMPLETED
//        );
//
//
//        /*
//         * 실제 텍스트 분석 DB 저장 확인
//         */
//        var analysis =
//                dreamAnalysisRepository
//                        .findByDreamId(
//                                dreamId
//                        )
//                        .orElseThrow();
//
//        assertThat(
//                analysis.getInterpretation()
//        )
//                .isNotNull()
//                .isNotBlank();
//
//
//        /*
//         * 감정 분석 저장 확인
//         */
//        var emotions =
//                dreamEmotionRepository
//                        .findAllByAnalysisId(
//                                analysis.getId()
//                        );
//
//        assertThat(emotions)
//                .isNotEmpty();
//
//
//        /*
//         * 요소 분석 저장 확인
//         */
//        var elements =
//                dreamElementRepository
//                        .findAllByAnalysisId(
//                                analysis.getId()
//                        );
//
//        assertThat(elements)
//                .isNotEmpty();
//
//
//        /*
//         * 실제 DreamImage DB 저장 확인
//         */
//        var images =
//                dreamImageRepository
//                        .findAllByDreamIdOrderByImageOrderAsc(
//                                dreamId
//                        );
//
//        assertThat(images)
//                .hasSize(1);
//
//
//        DreamImage image =
//                images.getFirst();
//
//        generatedImageKey =
//                image.getImageKey();
//
//
//        assertThat(generatedImageKey)
//                .startsWith(
//                        "dreams/"
//                                + dreamId
//                                + "/"
//                )
//                .endsWith(
//                        ".png"
//                );
//
//
//        /*
//         * 실제 R2에 이미지 파일이 존재하는지 확인
//         */
//        HeadObjectResponse head =
//                r2S3Client.headObject(
//                        HeadObjectRequest.builder()
//                                .bucket(
//                                        r2Properties.bucketName()
//                                )
//                                .key(
//                                        generatedImageKey
//                                )
//                                .build()
//                );
//
//
//        assertThat(
//                head.contentLength()
//        ).isGreaterThan(0);
//
//
//        assertThat(
//                head.contentType()
//        ).isEqualTo(
//                "image/png"
//        );
//
//
//        /*
//         * 성공했으므로 AI 사용량은 반환되지 않고
//         * 1회 사용 상태 그대로 남아 있어야 한다.
//         */
//        DreamAiUsage usage =
//                dreamAiUsageRepository
//                        .findByUserIdAndUsageDate(
//                                userId,
//                                usageDate
//                        )
//                        .orElseThrow();
//
//
//        assertThat(
//                usage.getUsedCount()
//        ).isEqualTo(1);
//
//
//        System.out.println(
//                "===================================="
//        );
//
//        System.out.println(
//                "AI STATUS = "
//                        + finishedDream.getAiStatus()
//        );
//
//        System.out.println(
//                "INTERPRETATION = "
//                        + analysis.getInterpretation()
//        );
//
//        System.out.println(
//                "EMOTION COUNT = "
//                        + emotions.size()
//        );
//
//        System.out.println(
//                "ELEMENT COUNT = "
//                        + elements.size()
//        );
//
//        System.out.println(
//                "IMAGE KEY = "
//                        + generatedImageKey
//        );
//
//        System.out.println(
//                "R2 IMAGE SIZE = "
//                        + head.contentLength()
//                        + " bytes"
//        );
//
//        System.out.println(
//                "===================================="
//        );
//    }
//
//
//    private Dream waitForAiCompletion(
//            Long dreamId,
//            Duration timeout
//    ) throws InterruptedException {
//
//        long deadline =
//                System.currentTimeMillis()
//                        + timeout.toMillis();
//
//
//        while (
//                System.currentTimeMillis()
//                        < deadline
//        ) {
//
//            Dream currentDream =
//                    dreamRepository
//                            .findById(
//                                    dreamId
//                            )
//                            .orElseThrow();
//
//
//            if (
//                    currentDream.getAiStatus()
//                            == DreamAiStatus.COMPLETED
//            ) {
//                return currentDream;
//            }
//
//
//            if (
//                    currentDream.getAiStatus()
//                            == DreamAiStatus.FAILED
//            ) {
//                fail(
//                        "AI 비동기 작업이 FAILED 상태로 종료되었습니다. "
//                                + "DreamAiAsyncService 로그를 확인하세요."
//                );
//            }
//
//
//            Thread.sleep(
//                    1000
//            );
//        }
//
//
//        fail(
//                "AI 작업이 "
//                        + timeout.toSeconds()
//                        + "초 안에 완료되지 않았습니다."
//        );
//
//        throw new IllegalStateException(
//                "unreachable"
//        );
//    }
//}