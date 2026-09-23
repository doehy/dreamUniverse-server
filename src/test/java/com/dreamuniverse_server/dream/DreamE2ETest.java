package com.dreamuniverse_server.dream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.dreamuniverse_server.dream.entity.DreamImage;
import com.dreamuniverse_server.dream.repository.DreamAiUsageRepository;
import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.image.service.ImageStorageService;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DreamE2ETest {

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
         * 매 테스트마다 완전히 새로운 MySQL Container이므로
         * 기존 테이블 삭제 없이 생성만 한다.
         */
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-only"
        );
    }


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DreamRepository dreamRepository;

    @Autowired
    private DreamAnalysisRepository dreamAnalysisRepository;

    @Autowired
    private DreamEmotionRepository dreamEmotionRepository;

    @Autowired
    private DreamElementRepository dreamElementRepository;

    @Autowired
    private DreamImageRepository dreamImageRepository;

    @Autowired
    private DreamAiUsageRepository dreamAiUsageRepository;


    @Autowired
    private ImageStorageService imageStorageService;


    private User user;

    private Long createdDreamId;


    @BeforeEach
    void setUp() {

        user = User.create(
                OAuthProvider.GOOGLE,
                "dream-e2e-test-user"
        );

        user.completeSignup(
                "dream-e2e@test.com",
                "Dream E2E Test User",
                "dream-e2e",
                LocalDate.of(
                        2000,
                        1,
                        1
                ),
                "Asia/Seoul",
                Instant.now()
        );

        user =
                userRepository.saveAndFlush(
                        user
                );
    }


    @AfterEach
    void tearDown() {

        /*
         * DB에서 삭제하기 전에
         * 실제 R2 이미지 key부터 가져온다.
         */
        if (createdDreamId != null) {

            List<DreamImage> images =
                    dreamImageRepository
                            .findAllByDreamIdOrderByImageOrderAsc(
                                    createdDreamId
                            );

            for (DreamImage image : images) {
                try {
                    imageStorageService.delete(
                            image.getImageKey()
                    );

                } catch (RuntimeException e) {
                    System.err.println(
                            "R2 cleanup failed: "
                                    + image.getImageKey()
                    );
                }
            }
        }


        /*
         * FK 때문에 자식부터 삭제
         */
        dreamEmotionRepository.deleteAll();
        dreamElementRepository.deleteAll();

        dreamImageRepository.deleteAll();
        dreamAnalysisRepository.deleteAll();

        dreamAiUsageRepository.deleteAll();

        dreamRepository.deleteAll();
        userRepository.deleteAll();
    }


    @Test
    void 꿈_API로_AI꿈을_생성하면_PROCESSING_응답후_COMPLETED가_된다()
            throws Exception {

        // given

        String requestBody = """
                {
                  "title": "하늘을 나는 꿈",
                  "content": "밤에 집 밖으로 나왔는데 갑자기 몸이 공중으로 떠올랐다. 나는 도시 위를 천천히 날기 시작했다. 아래에는 건물들의 불빛이 반짝이고 있었다. 계속 앞으로 날아가자 아주 거대한 보름달이 나타났다. 나는 놀라서 달을 바라보았다.",
                  "dreamDate": "2026-09-23",
                  "aiRequested": true
                }
                """;


        // when
        /*
         * 1. 실제 POST /dreams 요청
         */
        MvcResult createResult =
                mockMvc.perform(
                                post("/dreams")
                                        .with(
                                                jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        user.getId().toString()
                                                                )
                                                )
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                requestBody
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath("$.aiStatus")
                                        .value("PROCESSING")
                        )
                        .andReturn();


        /*
         * POST 응답에서 dreamId 추출
         */
        JsonNode createResponse =
                objectMapper.readTree(
                        createResult
                                .getResponse()
                                .getContentAsString()
                );


        createdDreamId =
                createResponse
                        .get("dreamId")
                        .asLong();


        assertThat(createdDreamId)
                .isPositive();


        System.out.println(
                "POST /dreams dreamId = "
                        + createdDreamId
        );

        System.out.println(
                "POST /dreams aiStatus = PROCESSING"
        );


        /*
         * 2. 비동기 AI 작업 완료까지 GET /dreams/{id} 반복
         */
        JsonNode completedDream =
                waitUntilCompleted(
                        createdDreamId
                );


        // then

        assertThat(
                completedDream
                        .get("aiStatus")
                        .asText()
        ).isEqualTo(
                "COMPLETED"
        );


        assertThat(
                completedDream
                        .get("title")
                        .asText()
        ).isEqualTo(
                "하늘을 나는 꿈"
        );


        /*
         * 분석 결과도 API로 실제 조회
         */
        MvcResult analysisResult =
                mockMvc.perform(
                                get(
                                        "/dreams/{dreamId}/analysis",
                                        createdDreamId
                                )
                                        .with(
                                                jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        user.getId().toString()
                                                                )
                                                )
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();


        JsonNode analysis =
                objectMapper.readTree(
                        analysisResult
                                .getResponse()
                                .getContentAsString()
                );


        assertThat(
                analysis
                        .get("interpretation")
                        .asText()
        )
                .isNotBlank();


        /*
         * 이미지 DB 저장까지 확인
         */
        List<DreamImage> images =
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(
                                createdDreamId
                        );


        assertThat(images)
                .hasSize(1);


        assertThat(
                images
                        .getFirst()
                        .getImageKey()
        )
                .startsWith(
                        "dreams/"
                                + createdDreamId
                                + "/"
                )
                .endsWith(
                        ".png"
                );


        System.out.println(
                "===================================="
        );

        System.out.println(
                "E2E DREAM ID = "
                        + createdDreamId
        );

        System.out.println(
                "FINAL AI STATUS = "
                        + completedDream
                        .get("aiStatus")
                        .asText()
        );

        System.out.println(
                "INTERPRETATION = "
                        + analysis
                        .get("interpretation")
                        .asText()
        );

        System.out.println(
                "IMAGE KEY = "
                        + images
                        .getFirst()
                        .getImageKey()
        );

        System.out.println(
                "===================================="
        );
    }


    private JsonNode waitUntilCompleted(
            Long dreamId
    ) throws Exception {

        /*
         * 이미지 생성 때문에 시간이 꽤 걸릴 수 있어서
         * 최대 2분 동안 기다린다.
         */
        long deadline =
                System.currentTimeMillis()
                        + 120_000;


        while (
                System.currentTimeMillis()
                        < deadline
        ) {

            MvcResult getResult =
                    mockMvc.perform(
                                    get(
                                            "/dreams/{dreamId}",
                                            dreamId
                                    )
                                            .with(
                                                    jwt().jwt(
                                                            jwt ->
                                                                    jwt.subject(
                                                                            user.getId().toString()
                                                                    )
                                                    )
                                            )
                            )
                            .andExpect(
                                    status().isOk()
                            )
                            .andReturn();


            JsonNode dreamResponse =
                    objectMapper.readTree(
                            getResult
                                    .getResponse()
                                    .getContentAsString()
                    );


            String aiStatus =
                    dreamResponse
                            .get("aiStatus")
                            .asText();


            System.out.println(
                    "GET /dreams/"
                            + dreamId
                            + " → "
                            + aiStatus
            );


            if (
                    "COMPLETED".equals(
                            aiStatus
                    )
            ) {
                return dreamResponse;
            }


            if (
                    "FAILED".equals(
                            aiStatus
                    )
            ) {
                throw new AssertionError(
                        "AI 작업이 FAILED로 종료되었습니다. "
                                + "DreamAiAsyncService 로그를 확인하세요."
                );
            }


            Thread.sleep(
                    1000
            );
        }


        throw new AssertionError(
                "AI 작업이 120초 안에 완료되지 않았습니다."
        );
    }
}