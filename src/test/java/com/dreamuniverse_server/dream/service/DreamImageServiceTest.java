package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamImageClient;
import com.dreamuniverse_server.dream.dto.ai.DreamImageResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.policy.AdultPolicy;
import com.dreamuniverse_server.dream.policy.SubscriptionPolicy;
import com.dreamuniverse_server.dream.prompt.DreamImagePromptBuilder;
import com.dreamuniverse_server.image.service.ImageStorageService;
import com.dreamuniverse_server.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DreamImageServiceTest {

    @Mock
    private AdultPolicy adultPolicy;

    @Mock
    private SubscriptionPolicy subscriptionPolicy;

    @Mock
    private DreamImagePromptBuilder dreamImagePromptBuilder;

    @Mock
    private DreamImageClient dreamImageClient;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private User user;

    @Mock
    private Dream dream;

    private DreamImageService dreamImageService;


    @BeforeEach
    void setUp() {
        dreamImageService =
                new DreamImageService(
                        adultPolicy,
                        subscriptionPolicy,
                        dreamImagePromptBuilder,
                        dreamImageClient,
                        imageStorageService
                );
    }


    @Test
    void 이미지를_생성하고_R2에_업로드한_뒤_imageKey를_반환한다() {

        // given
        byte[] imageBytes =
                new byte[]{1, 2, 3};

        given(user.getTimezone())
                .willReturn("Asia/Seoul");

        given(user.getBirthDate())
                .willReturn(
                        LocalDate.of(2000, 1, 1)
                );

        given(dream.getId())
                .willReturn(10L);

        given(dream.getContent())
                .willReturn(
                        "하늘을 날아다니는 꿈"
                );

        given(
                adultPolicy.isAdult(
                        eq(LocalDate.of(2000, 1, 1)),
                        any(LocalDate.class)
                )
        ).willReturn(true);

        given(
                subscriptionPolicy.getMaxImageCount(user)
        ).willReturn(1);

        given(
                dreamImagePromptBuilder.build(
                        "하늘을 날아다니는 꿈",
                        true,
                        1
                )
        ).willReturn(
                "image prompt"
        );

        given(
                dreamImageClient.generate(
                        "image prompt"
                )
        ).willReturn(
                new DreamImageResult(
                        imageBytes
                )
        );


        // when
        String imageKey =
                dreamImageService.generateAndUpload(
                        user,
                        dream
                );


        // then
        assertThat(imageKey)
                .startsWith(
                        "dreams/10/"
                )
                .endsWith(
                        ".png"
                );

        verify(imageStorageService)
                .upload(
                        imageBytes,
                        imageKey
                );
    }


    @Test
    void OpenAI_이미지_생성에_실패하면_R2를_호출하지_않는다() {

        // given
        given(user.getTimezone())
                .willReturn("Asia/Seoul");

        given(user.getBirthDate())
                .willReturn(
                        LocalDate.of(2000, 1, 1)
                );

        given(dream.getContent())
                .willReturn(
                        "꿈 내용"
                );

        given(
                adultPolicy.isAdult(
                        eq(LocalDate.of(2000, 1, 1)),
                        any(LocalDate.class)
                )
        ).willReturn(true);

        given(
                subscriptionPolicy.getMaxImageCount(user)
        ).willReturn(1);

        given(
                dreamImagePromptBuilder.build(
                        "꿈 내용",
                        true,
                        1
                )
        ).willReturn(
                "image prompt"
        );

        given(
                dreamImageClient.generate(
                        "image prompt"
                )
        ).willThrow(
                new RuntimeException(
                        "OpenAI failure"
                )
        );


        // when & then
        assertThatThrownBy(
                () -> dreamImageService.generateAndUpload(
                        user,
                        dream
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OpenAI failure");

        verifyNoInteractions(
                imageStorageService
        );
    }


    @Test
    void R2_업로드에_실패하면_예외를_전파한다() {

        // given
        byte[] imageBytes =
                new byte[]{1, 2, 3};

        given(user.getTimezone())
                .willReturn("Asia/Seoul");

        given(user.getBirthDate())
                .willReturn(
                        LocalDate.of(2000, 1, 1)
                );

        given(dream.getId())
                .willReturn(10L);

        given(dream.getContent())
                .willReturn(
                        "꿈 내용"
                );

        given(
                adultPolicy.isAdult(
                        eq(LocalDate.of(2000, 1, 1)),
                        any(LocalDate.class)
                )
        ).willReturn(true);

        given(
                subscriptionPolicy.getMaxImageCount(user)
        ).willReturn(1);

        given(
                dreamImagePromptBuilder.build(
                        "꿈 내용",
                        true,
                        1
                )
        ).willReturn(
                "image prompt"
        );

        given(
                dreamImageClient.generate(
                        "image prompt"
                )
        ).willReturn(
                new DreamImageResult(
                        imageBytes
                )
        );


        ArgumentCaptor<String> imageKeyCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );

        org.mockito.Mockito
                .doThrow(
                        new RuntimeException(
                                "R2 upload failure"
                        )
                )
                .when(imageStorageService)
                .upload(
                        eq(imageBytes),
                        imageKeyCaptor.capture()
                );


        // when & then
        assertThatThrownBy(
                () -> dreamImageService.generateAndUpload(
                        user,
                        dream
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("R2 upload failure");


        assertThat(
                imageKeyCaptor.getValue()
        )
                .startsWith(
                        "dreams/10/"
                )
                .endsWith(
                        ".png"
                );
    }
}