package com.dreamuniverse_server.dream.policy;

import com.dreamuniverse_server.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPolicy {

    private static final int FREE_MAX_IMAGE_COUNT = 1;
    private static final int PREMIUM_MAX_IMAGE_COUNT = 3;

    public boolean isPremium(User user) {

        /*
         * TODO
         * 구독 기능 구현 후 실제 구독 상태를 확인하도록 변경한다.
         *
         * 현재 DreamUniverse MVP에는 유료 구독 기능이 없으므로
         * 모든 사용자를 무료 사용자로 취급한다.
         */
        return false;
    }

    public int getMaxImageCount(User user) {

        return isPremium(user)
                ? PREMIUM_MAX_IMAGE_COUNT
                : FREE_MAX_IMAGE_COUNT;
    }
}