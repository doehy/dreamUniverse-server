package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamImageClient;
import com.dreamuniverse_server.dream.dto.ai.DreamImageResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamImage;
import com.dreamuniverse_server.dream.policy.AdultPolicy;
import com.dreamuniverse_server.dream.policy.SubscriptionPolicy;
import com.dreamuniverse_server.dream.prompt.DreamImagePromptBuilder;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import com.dreamuniverse_server.image.service.ImageStorageService;
import com.dreamuniverse_server.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
    private DreamImageRepository dreamImageRepository;

    @Mock
    private DreamImagePersistenceService dreamImagePersistenceService;

    @Mock
    private User user;

    @Mock
    private Dream dream;

    @Mock
    private DreamImage oldDreamImage;

    @Mock
    private DreamImage savedDreamImage;


    private DreamImageService dreamImageService;


    @BeforeEach
    void setUp() {
        dreamImageService =
                new DreamImageService(
                        adultPolicy,
                        subscriptionPolicy,
                        dreamImagePromptBuilder,
                        dreamImageClient,
                        imageStorageService,
                        dreamImageRepository,
                        dreamImagePersistenceService
                );
    }


    @Test
    void 이미지를_생성하고_R2에_업로드한_뒤_DB에_저장한다() {

        // given
        byte[] imageBytes = new byte[]{1, 2, 3};

        given(user.getTimezone())
                .willReturn("Asia/Seoul");

        given(user.getBirthDate())
                .willReturn(
                        LocalDate.of(2000, 1, 1)
                );

        given(dream.getId())
                .willReturn(10L);

        given(dream.getContent())
                .willReturn("하늘을 날아다니는 꿈");

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
        ).willReturn("image prompt");

        given(
                dreamImageClient.generate(
                        "image prompt"
                )
        ).willReturn(
                new DreamImageResult(
                        imageBytes
                )
        );

        given(
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(10L)
        ).willReturn(
                List.of(oldDreamImage)
        );

        given(oldDreamImage.getImageKey())
                .willReturn(
                        "dreams/10/old.png"
                );

        given(
                dreamImagePersistenceService.replace(
                        eq(dream),
                        anyString()
                )
        ).willReturn(
                savedDreamImage
        );


        // when
        DreamImage result =
                dreamImageService.generateAndSave(
                        user,
                        dream
                );


        // then
        assertThat(result)
                .isSameAs(savedDreamImage);


        ArgumentCaptor<String> imageKeyCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(imageStorageService)
                .upload(
                        eq(imageBytes),
                        imageKeyCaptor.capture()
                );


        String newImageKey =
                imageKeyCaptor.getValue();


        assertThat(newImageKey)
                .startsWith(
                        "dreams/10/"
                )
                .endsWith(
                        ".png"
                );


        verify(dreamImagePersistenceService)
                .replace(
                        dream,
                        newImageKey
                );


        verify(imageStorageService)
                .delete(
                        "dreams/10/old.png"
                );
    }


    @Test
    void DB_저장에_실패하면_새로_업로드한_R2_이미지를_삭제한다() {

        // given
        byte[] imageBytes = new byte[]{1, 2, 3};

        given(user.getTimezone())
                .willReturn("Asia/Seoul");

        given(user.getBirthDate())
                .willReturn(
                        LocalDate.of(2000, 1, 1)
                );

        given(dream.getId())
                .willReturn(10L);

        given(dream.getContent())
                .willReturn("꿈 내용");

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

        given(
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(10L)
        ).willReturn(
                List.of()
        );

        given(
                dreamImagePersistenceService.replace(
                        eq(dream),
                        anyString()
                )
        ).willThrow(
                new RuntimeException(
                        "DB failure"
                )
        );


        // when & then
        assertThatThrownBy(
                () -> dreamImageService.generateAndSave(
                        user,
                        dream
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB failure");


        ArgumentCaptor<String> imageKeyCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(imageStorageService)
                .upload(
                        eq(imageBytes),
                        imageKeyCaptor.capture()
                );


        String uploadedImageKey =
                imageKeyCaptor.getValue();


        verify(imageStorageService)
                .delete(
                        uploadedImageKey
                );
    }


    @Test
    void 기존_R2_이미지_삭제에_실패해도_새_이미지_저장은_성공한다() {

        // given
        byte[] imageBytes = new byte[]{1, 2, 3};

        given(user.getTimezone())
                .willReturn("Asia/Seoul");

        given(user.getBirthDate())
                .willReturn(
                        LocalDate.of(2000, 1, 1)
                );

        given(dream.getId())
                .willReturn(10L);

        given(dream.getContent())
                .willReturn("꿈 내용");

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

        given(
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(10L)
        ).willReturn(
                List.of(oldDreamImage)
        );

        given(oldDreamImage.getImageKey())
                .willReturn(
                        "dreams/10/old.png"
                );

        given(
                dreamImagePersistenceService.replace(
                        eq(dream),
                        anyString()
                )
        ).willReturn(
                savedDreamImage
        );

        doThrow(
                new RuntimeException(
                        "R2 delete failure"
                )
        )
                .when(imageStorageService)
                .delete(
                        "dreams/10/old.png"
                );


        // when
        DreamImage result =
                dreamImageService.generateAndSave(
                        user,
                        dream
                );


        // then
        assertThat(result)
                .isSameAs(
                        savedDreamImage
                );

        verify(imageStorageService)
                .delete(
                        "dreams/10/old.png"
                );
    }


    @Test
    void OpenAI_이미지_생성에_실패하면_R2와_DB를_호출하지_않는다() {

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
                () -> dreamImageService.generateAndSave(
                        user,
                        dream
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OpenAI failure");


        verifyNoInteractions(
                imageStorageService
        );

        verifyNoInteractions(
                dreamImagePersistenceService
        );

        verifyNoInteractions(
                dreamImageRepository
        );
    }


    @Test
    void R2_업로드에_실패하면_DB에_저장하지_않는다() {

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

        given(
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(10L)
        ).willReturn(
                List.of()
        );

        doThrow(
                new RuntimeException(
                        "R2 upload failure"
                )
        )
                .when(imageStorageService)
                .upload(
                        eq(imageBytes),
                        anyString()
                );


        // when & then
        assertThatThrownBy(
                () -> dreamImageService.generateAndSave(
                        user,
                        dream
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("R2 upload failure");


        verifyNoInteractions(
                dreamImagePersistenceService
        );
    }
}