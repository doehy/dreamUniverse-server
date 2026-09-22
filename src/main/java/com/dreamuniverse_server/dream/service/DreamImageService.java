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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DreamImageService {

    private final AdultPolicy adultPolicy;
    private final SubscriptionPolicy subscriptionPolicy;

    private final DreamImagePromptBuilder dreamImagePromptBuilder;
    private final DreamImageClient dreamImageClient;

    private final ImageStorageService imageStorageService;

    private final DreamImageRepository dreamImageRepository;
    private final DreamImagePersistenceService dreamImagePersistenceService;


    public DreamImage generateAndSave(
            User user,
            Dream dream
    ) {
        boolean adult =
                isAdult(user);

        int maxImageCount =
                subscriptionPolicy.getMaxImageCount(
                        user
                );

        String prompt =
                dreamImagePromptBuilder.build(
                        dream.getContent(),
                        adult,
                        maxImageCount
                );

        DreamImageResult result =
                dreamImageClient.generate(
                        prompt
                );


        String imageKey =
                createImageKey(
                        dream.getId()
                );


        /*
         * DB 교체가 성공한 뒤 기존 R2 이미지를 삭제하기 위해
         * 먼저 기존 imageKey를 확보해둔다.
         */
        List<String> oldImageKeys =
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(
                                dream.getId()
                        )
                        .stream()
                        .map(DreamImage::getImageKey)
                        .toList();


        /*
         * 새 이미지를 먼저 R2에 업로드한다.
         */
        imageStorageService.upload(
                result.imageBytes(),
                imageKey
        );


        DreamImage savedImage;

        try {

            /*
             * DB에서는
             *
             * 기존 DreamImage 삭제
             *        ↓
             * 새 DreamImage 저장
             *
             * 을 하나의 짧은 Transaction으로 처리한다.
             */
            savedImage =
                    dreamImagePersistenceService.replace(
                            dream,
                            imageKey
                    );

        } catch (RuntimeException e) {

            /*
             * R2 업로드는 성공했는데
             * DB 저장이 실패했다면
             *
             * 새로 업로드한 이미지는 orphan이 되므로 삭제한다.
             */
            try {
                imageStorageService.delete(
                        imageKey
                );
            } catch (RuntimeException deleteException) {
                log.error(
                        "Failed to clean up newly uploaded dream image. imageKey={}",
                        imageKey,
                        deleteException
                );
            }

            throw e;
        }


        /*
         * DB가 새 이미지로 정상 교체된 이후에
         * 기존 R2 이미지들을 제거한다.
         */
        deleteOldImages(
                oldImageKeys
        );


        return savedImage;
    }


    private boolean isAdult(
            User user
    ) {
        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                user.getTimezone()
                        )
                );

        return adultPolicy.isAdult(
                user.getBirthDate(),
                today
        );
    }


    private String createImageKey(
            Long dreamId
    ) {
        return "dreams/%d/%s.png"
                .formatted(
                        dreamId,
                        UUID.randomUUID()
                );
    }


    private void deleteOldImages(
            List<String> oldImageKeys
    ) {
        for (String oldImageKey : oldImageKeys) {
            try {
                imageStorageService.delete(
                        oldImageKey
                );
            } catch (RuntimeException e) {
                /*
                 * DB는 이미 새 이미지로 정상 전환되었다.
                 *
                 * 여기서 기존 R2 파일 삭제가 실패했다고
                 * 전체 AI 완성을 실패 처리하지 않는다.
                 *
                 * 기존 파일은 orphan이 되므로
                 * 추후 cleanup 대상으로 처리할 수 있다.
                 */
                log.error(
                        "Failed to delete old dream image from R2. imageKey={}",
                        oldImageKey,
                        e
                );
            }
        }
    }
}