
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.ad.service.RewardedAdSessionService;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.policy.DreamUsagePolicy;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DreamAiRequestService {

    private final DreamRepository dreamRepository;
    private final DreamAiUsageService dreamAiUsageService;
    private final DreamEntitlementService dreamEntitlementService;
    private final RewardedAdSessionService rewardedAdSessionService;

    /**
     * AI 분석 요청 준비
     *
     * 1. 꿈 잠금 및 중복 분석 방지
     * 2. 일일 AI 사용량 차감
     * 3. 광고 필요 시 검증된 보상 1회 소비
     * 4. PROCESSING 전환
     *
     * 하나의 트랜잭션으로 처리한다.
     */
    @Transactional
    public LocalDate prepare(
            Long userId,
            User user,
            Long dreamId,
            String adSessionId
    ) {

        // 1. 해당 꿈 행 잠금
        Dream dream = dreamRepository
                .findByIdAndUserIdForUpdate(
                        dreamId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DREAM_NOT_FOUND
                        )
                );

        // 2. 이미 분석 중이면 중복 요청 차단
        if (dream.getAiStatus() == DreamAiStatus.PROCESSING) {
            throw new CustomException(
                    ErrorCode.DREAM_AI_ALREADY_PROCESSING
            );
        }

        // 3. 해당 사용자의 구독 정책 조회
        DreamUsagePolicy policy =
                dreamEntitlementService.getPolicy(userId);

        // 4. 광고가 필요한 사용자라면 세션 ID 확인
        if (policy.adRequired()
                && (adSessionId == null || adSessionId.isBlank())) {
            throw new CustomException(
                    ErrorCode.AD_REWARD_REQUIRED
            );
        }

        // 5. 일일 AI 사용량 1회 차감
        LocalDate usageDate =
                dreamAiUsageService.acquire(user);

        // 6. 무료 사용자라면 검증된 광고 보상 소비
        if (policy.adRequired()) {
            rewardedAdSessionService.consumeVerifiedReward(
                    userId,
                    adSessionId
            );
        }

        // 7. PROCESSING 전환
        dream.startAiCompletion();

        return usageDate;
    }
}
