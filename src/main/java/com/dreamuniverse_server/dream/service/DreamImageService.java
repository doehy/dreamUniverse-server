package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamImageClient;
import com.dreamuniverse_server.dream.dto.ai.DreamImageResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.policy.AdultPolicy;
import com.dreamuniverse_server.dream.policy.SubscriptionPolicy;
import com.dreamuniverse_server.dream.prompt.DreamImagePromptBuilder;
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

    public String generateAndUpload(
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

        imageStorageService.upload(
                result.imageBytes(),
                imageKey
        );

        return imageKey;
    }

    public void deleteUploadedImage(
            String imageKey
    ) {
        try {
            imageStorageService.delete(
                    imageKey
            );

        } catch (RuntimeException e) {
            log.error(
                    "Failed to clean up dream image. imageKey={}",
                    imageKey,
                    e
            );
        }
    }

    public void deleteOldImages(
            List<String> imageKeys
    ) {
        for (String imageKey : imageKeys) {
            try {
                imageStorageService.delete(
                        imageKey
                );

            } catch (RuntimeException e) {
                log.error(
                        "Failed to delete old dream image. imageKey={}",
                        imageKey,
                        e
                );
            }
        }
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
}