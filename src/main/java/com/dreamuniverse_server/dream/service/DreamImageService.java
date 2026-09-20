package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamImageClient;
import com.dreamuniverse_server.dream.dto.ai.DreamImageResult;
import com.dreamuniverse_server.dream.policy.AdultPolicy;
import com.dreamuniverse_server.dream.policy.SubscriptionPolicy;
import com.dreamuniverse_server.dream.prompt.DreamImagePromptBuilder;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DreamImageService {

    private final AdultPolicy adultPolicy;
    private final SubscriptionPolicy subscriptionPolicy;
    private final DreamImagePromptBuilder dreamImagePromptBuilder;
    private final DreamImageClient dreamImageClient;

    public DreamImageResult generate(
            User user,
            String dreamContent
    ) {
        LocalDate today =
                LocalDate.now(
                        ZoneId.of(user.getTimezone())
                );

        boolean adult =
                adultPolicy.isAdult(
                        user.getBirthDate(),
                        today
                );

        int maxImageCount =
                subscriptionPolicy.getMaxImageCount(user);

        String prompt =
                dreamImagePromptBuilder.build(
                        dreamContent,
                        adult,
                        maxImageCount
                );

        return dreamImageClient.generate(prompt);
    }
}