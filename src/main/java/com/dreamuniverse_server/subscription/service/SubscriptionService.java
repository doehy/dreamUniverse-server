package com.dreamuniverse_server.subscription.service;

import com.dreamuniverse_server.subscription.entity.Subscription;
import com.dreamuniverse_server.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;


    @Transactional(readOnly = true)
    public boolean isPremium(Long userId) {

        LocalDateTime now =
                LocalDateTime.now(clock);

        return subscriptionRepository
                .findByUserId(userId)
                .map(subscription ->
                        subscription.isPremium(now)
                )
                .orElse(false);
    }
}