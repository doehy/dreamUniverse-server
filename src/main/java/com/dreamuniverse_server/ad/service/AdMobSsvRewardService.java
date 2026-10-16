package com.dreamuniverse_server.ad.service;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import com.dreamuniverse_server.ad.entity.RewardedAdStatus;
import com.dreamuniverse_server.ad.repository.RewardedAdSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdMobSsvRewardService {

    private final RewardedAdSessionRepository sessionRepository;

    @Value("${app.admob.rewarded-ad-unit-id:}")
    private String rewardedAdUnitId;

    @Value("${app.admob.reward-item:ai_completion}")
    private String expectedRewardItem;

    @Value("${app.admob.reward-amount:1}")
    private long expectedRewardAmount;

    @Transactional
    public void grant(
            AdMobSsvVerifier.VerifiedCallback callback
    ) {

        // 1. 우리 앱의 보상형 광고 단위인지 확인
        if (!isValidAdUnit(callback.adUnit())) {
            log.warn("[AdMob SSV] Unrecognized ad unit");
            return;
        }

        // 2. AdMob에 설정한 보상 상품 및 수량 확인
        if (!expectedRewardItem.equals(callback.rewardItem())
                || expectedRewardAmount != callback.rewardAmount()) {

            log.warn("[AdMob SSV] Invalid reward configuration");
            return;
        }

        // 3. custom_data에 담긴 세션 ID 형식 확인
        try {
            UUID.fromString(callback.sessionId());
        } catch (IllegalArgumentException e) {
            log.warn("[AdMob SSV] Invalid session ID");
            return;
        }

        // 4. 광고 보상 발생 시각 확인
        Instant rewardTime;

        try {
            rewardTime = Instant.ofEpochMilli(
                    callback.timestampMillis()
            );
        } catch (RuntimeException e) {
            log.warn("[AdMob SSV] Invalid timestamp");
            return;
        }

        Instant now = Instant.now();

        if (rewardTime.isAfter(now.plusSeconds(120))) {
            log.warn("[AdMob SSV] Future reward timestamp");
            return;
        }

        // 5. 세션 행 잠금
        RewardedAdSession session = sessionRepository
                .findByIdForUpdate(callback.sessionId())
                .orElse(null);

        if (session == null) {
            log.warn("[AdMob SSV] Unknown session");
            return;
        }

        // 6. 동일 거래의 재전송이면 무시
        if (callback.transactionId().equals(
                session.getAdmobTransactionId())) {
            return;
        }

        // 7. PENDING 상태인지 확인
        if (session.getStatus() != RewardedAdStatus.PENDING) {
            log.warn("[AdMob SSV] Session already processed");
            return;
        }

        // 8. 검증 대기 시간이 끝났는지 확인
        if (!now.isBefore(
                session.getVerificationExpiresAt())) {
            log.warn("[AdMob SSV] Session expired");
            return;
        }

        // 9. 세션 생성 이전의 광고 이벤트인지 확인
        if (rewardTime.isBefore(session.getCreatedAt())) {
            log.warn("[AdMob SSV] Reward predates session");
            return;
        }

        // 10. 중복 거래 ID 확인
        if (sessionRepository.existsByAdmobTransactionId(
                callback.transactionId())) {

            log.warn("[AdMob SSV] Duplicate transaction");
            return;
        }

        // 11. Google 검증이 완료된 보상 지급
        session.verify(callback.transactionId(), now);

        log.info(
                "[AdMob SSV] Reward verified. sessionId={}",
                session.getId()
        );
    }

    /**
     * 실제 광고 단위 ID:
     * ca-app-pub-.../9405922508
     *
     * SSV ad_unit:
     * 9405922508
     *
     * 두 값을 비교한다.
     */
    private boolean isValidAdUnit(String callbackAdUnit) {

        if (rewardedAdUnitId == null
                || rewardedAdUnitId.isBlank()
                || callbackAdUnit == null
                || callbackAdUnit.isBlank()) {
            return false;
        }

        int separator = rewardedAdUnitId.lastIndexOf('/');

        if (separator < 0
                || separator == rewardedAdUnitId.length() - 1) {
            return false;
        }

        String expectedAdUnit =
                rewardedAdUnitId.substring(separator + 1);

        return expectedAdUnit.equals(callbackAdUnit);
    }
}