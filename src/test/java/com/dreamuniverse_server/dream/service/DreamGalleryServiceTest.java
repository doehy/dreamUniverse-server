//package com.dreamuniverse_server.dream.service;
//
//import com.dreamuniverse_server.dream.dto.response.DreamGalleryResponse;
//import com.dreamuniverse_server.dream.entity.Dream;
//import com.dreamuniverse_server.dream.entity.DreamImage;
//import com.dreamuniverse_server.dream.repository.DreamImageRepository;
//import com.dreamuniverse_server.dream.repository.DreamRepository;
//import com.dreamuniverse_server.image.service.ImageStorageService;
//import com.dreamuniverse_server.user.entity.OAuthProvider;
//import com.dreamuniverse_server.user.entity.User;
//import com.dreamuniverse_server.user.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.context.annotation.Primary;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.time.Instant;
//import java.time.LocalDate;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@Testcontainers
//@DataJpaTest
//@Import({
//        DreamGalleryService.class,
//        DreamGalleryServiceTest.MockConfig.class
//})
//class DreamGalleryServiceTest {
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
//    private DreamGalleryService dreamGalleryService;
//
//    @Autowired
//    private DreamRepository dreamRepository;
//
//    @Autowired
//    private DreamImageRepository dreamImageRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private ImageStorageService imageStorageService;
//
//
//    private User user;
//    private User otherUser;
//
//
//    @BeforeEach
//    void setUp() {
//
//        user = createUser(
//                "gallery-user",
//                "gallery@test.com",
//                "gallery"
//        );
//
//        otherUser = createUser(
//                "gallery-other-user",
//                "gallery-other@test.com",
//                "gallery-other"
//        );
//
//
//        when(
//                imageStorageService
//                        .generatePresignedGetUrl(
//                                anyString()
//                        )
//        ).thenAnswer(
//                invocation ->
//                        "https://presigned.test/"
//                                + invocation.getArgument(
//                                0,
//                                String.class
//                        )
//        );
//    }
//
//
//    @Test
//    void 갤러리는_내_COMPLETED_꿈의_대표이미지만_조회한다() {
//
//        // given
//
//        Dream completedDream =
//                createCompletedDream(
//                        user,
//                        "완료된 꿈",
//                        LocalDate.of(
//                                2026,
//                                9,
//                                29
//                        )
//                );
//
//        /*
//         * 대표 이미지
//         */
//        dreamImageRepository.save(
//                DreamImage.create(
//                        completedDream,
//                        "dreams/"
//                                + completedDream.getId()
//                                + "/main.png",
//                        1
//                )
//        );
//
//        /*
//         * 같은 꿈의 두 번째 이미지.
//         * 갤러리에서는 제외되어야 한다.
//         */
//        dreamImageRepository.save(
//                DreamImage.create(
//                        completedDream,
//                        "dreams/"
//                                + completedDream.getId()
//                                + "/second.png",
//                        2
//                )
//        );
//
//
//        Dream processingDream =
//                createProcessingDream(
//                        user,
//                        "처리 중 꿈",
//                        LocalDate.of(
//                                2026,
//                                9,
//                                28
//                        )
//                );
//
//        /*
//         * PROCESSING 꿈의 이미지는 조회되면 안 된다.
//         */
//        dreamImageRepository.save(
//                DreamImage.create(
//                        processingDream,
//                        "dreams/"
//                                + processingDream.getId()
//                                + "/processing.png",
//                        1
//                )
//        );
//
//
//        Dream otherDream =
//                createCompletedDream(
//                        otherUser,
//                        "다른 사용자 꿈",
//                        LocalDate.of(
//                                2026,
//                                9,
//                                29
//                        )
//                );
//
//        /*
//         * 다른 사용자의 이미지는 조회되면 안 된다.
//         */
//        dreamImageRepository.save(
//                DreamImage.create(
//                        otherDream,
//                        "dreams/"
//                                + otherDream.getId()
//                                + "/other.png",
//                        1
//                )
//        );
//
//        dreamImageRepository.flush();
//
//
//        // when
//
//        DreamGalleryResponse response =
//                dreamGalleryService.getGallery(
//                        user.getId(),
//                        null,
//                        6
//                );
//
//
//        // then
//
//        assertThat(
//                response.images()
//        ).hasSize(
//                1
//        );
//
//
//        assertThat(
//                response.images()
//                        .getFirst()
//                        .dreamId()
//        ).isEqualTo(
//                completedDream.getId()
//        );
//
//
//        assertThat(
//                response.images()
//                        .getFirst()
//                        .dreamDate()
//        ).isEqualTo(
//                LocalDate.of(
//                        2026,
//                        9,
//                        29
//                )
//        );
//
//
//        assertThat(
//                response.images()
//                        .getFirst()
//                        .imageUrl()
//        ).isEqualTo(
//                "https://presigned.test/dreams/"
//                        + completedDream.getId()
//                        + "/main.png"
//        );
//
//
//        assertThat(
//                response.hasNext()
//        ).isFalse();
//
//
//        assertThat(
//                response.nextCursor()
//        ).isNull();
//    }
//
//
//    @Test
//    void size가_6이면_6개만_반환하고_nextCursor로_다음페이지를_조회한다() {
//
//        // given
//
//        /*
//         * 총 8개의 완료된 꿈 생성.
//         *
//         * 각 꿈은 날짜가 달라야 한다.
//         * Dream에 (user_id, dream_date) UNIQUE가 있기 때문.
//         */
//        for (int i = 0; i < 8; i++) {
//
//            Dream dream =
//                    createCompletedDream(
//                            user,
//                            "꿈 " + i,
//                            LocalDate.of(
//                                    2026,
//                                    9,
//                                    20
//                            ).plusDays(i)
//                    );
//
//            dreamImageRepository.save(
//                    DreamImage.create(
//                            dream,
//                            "dreams/"
//                                    + dream.getId()
//                                    + "/image.png",
//                            1
//                    )
//            );
//        }
//
//        dreamImageRepository.flush();
//
//
//        // when - 첫 페이지
//
//        DreamGalleryResponse firstPage =
//                dreamGalleryService.getGallery(
//                        user.getId(),
//                        null,
//                        6
//                );
//
//
//        // then - 첫 페이지
//
//        assertThat(
//                firstPage.images()
//        ).hasSize(
//                6
//        );
//
//        assertThat(
//                firstPage.hasNext()
//        ).isTrue();
//
//        assertThat(
//                firstPage.nextCursor()
//        ).isNotNull();
//
//
//        // when - 두 번째 페이지
//
//        DreamGalleryResponse secondPage =
//                dreamGalleryService.getGallery(
//                        user.getId(),
//                        firstPage.nextCursor(),
//                        6
//                );
//
//
//        // then - 두 번째 페이지
//
//        assertThat(
//                secondPage.images()
//        ).hasSize(
//                2
//        );
//
//        assertThat(
//                secondPage.hasNext()
//        ).isFalse();
//
//        assertThat(
//                secondPage.nextCursor()
//        ).isNull();
//
//
//        /*
//         * 첫 페이지와 두 번째 페이지에
//         * 같은 dreamId가 없어야 한다.
//         */
//        assertThat(
//                firstPage.images()
//                        .stream()
//                        .map(
//                                image ->
//                                        image.dreamId()
//                        )
//                        .toList()
//        ).doesNotContainAnyElementsOf(
//                secondPage.images()
//                        .stream()
//                        .map(
//                                image ->
//                                        image.dreamId()
//                        )
//                        .toList()
//        );
//    }
//
//
//    private User createUser(
//            String providerUserId,
//            String email,
//            String nickname
//    ) {
//
//        User newUser =
//                User.create(
//                        OAuthProvider.GOOGLE,
//                        providerUserId
//                );
//
//        newUser.completeSignup(
//                email,
//                "Test User",
//                nickname,
//                LocalDate.of(
//                        2000,
//                        1,
//                        1
//                ),
//                "Asia/Seoul",
//                Instant.now()
//        );
//
//        return userRepository.saveAndFlush(
//                newUser
//        );
//    }
//
//
//    private Dream createCompletedDream(
//            User user,
//            String title,
//            LocalDate dreamDate
//    ) {
//
//        Dream dream =
//                Dream.create(
//                        user,
//                        title,
//                        "테스트 꿈 내용",
//                        dreamDate
//                );
//
//        dream.completeAiCompletion();
//
//        return dreamRepository.saveAndFlush(
//                dream
//        );
//    }
//
//
//    private Dream createProcessingDream(
//            User user,
//            String title,
//            LocalDate dreamDate
//    ) {
//
//        Dream dream =
//                Dream.create(
//                        user,
//                        title,
//                        "테스트 꿈 내용",
//                        dreamDate
//                );
//
//        dream.startAiCompletion();
//
//        return dreamRepository.saveAndFlush(
//                dream
//        );
//    }
//
//
//    @TestConfiguration
//    static class MockConfig {
//
//        @Bean
//        @Primary
//        ImageStorageService imageStorageService() {
//            return mock(
//                    ImageStorageService.class
//            );
//        }
//    }
//}