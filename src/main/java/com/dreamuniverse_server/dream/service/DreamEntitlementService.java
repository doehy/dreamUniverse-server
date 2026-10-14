package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.policy.DreamUsagePolicy;
import com.dreamuniverse_server.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DreamEntitlementService {

    private static final DreamUsagePolicy FREE_POLICY =
            new DreamUsagePolicy(
                    3,
                    4,
                    true
            );

    private static final DreamUsagePolicy PREMIUM_POLICY =
            new DreamUsagePolicy(
                    3,
                    8,
                    false
            );

    private final SubscriptionService subscriptionService;


    public DreamUsagePolicy getPolicy(
            Long userId
    ) {
        if (subscriptionService.isPremium(userId)) {
            return PREMIUM_POLICY;
        }

        return FREE_POLICY;
    }
}