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

    @Transactional
    public void grant(
            AdMobSsvVerifier.VerifiedCallback callback
    ) {
        // 1. 우리 앱의 보상형 광고 단위인지 확인
        if (rewardedAdUnitId.isBlank()
                || !rewardedAdUnitId.equals(callback.adUnit())) {
            log.warn("[AdMob SSV] Unrecognized ad unit");
            return;
        }

        // 2. custom_data에 담긴 세션 ID 검증
        try {
            UUID.fromString(callback.sessionId());
        } catch (IllegalArgumentException e) {
            log.warn("[AdMob SSV] Invalid session ID");
            return;
        }

        // 3. 서명된 광고 발생 시각 확인
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

        // 미래에 생성된 것으로 보이는 이벤트 차단
        if (rewardTime.isAfter(now.plusSeconds(120))) {
            log.warn("[AdMob SSV] Future reward timestamp");
            return;
        }

        // 4. 세션 행 잠금
        RewardedAdSession session = sessionRepository
                .findByIdForUpdate(callback.sessionId())
                .orElse(null);

        if (session == null) {
            log.warn("[AdMob SSV] Unknown session");
            return;
        }

        // 5. 동일한 거래의 재전송은 정상적으로 무시
        if (callback.transactionId().equals(
                session.getAdmobTransactionId())) {
            return;
        }

        // 6. 보상 가능 상태 확인
        if (session.getStatus() != RewardedAdStatus.PENDING) {
            log.warn("[AdMob SSV] Session already processed");
            return;
        }

        if (!now.isBefore(
                session.getVerificationExpiresAt())) {
            log.warn("[AdMob SSV] Session expired");
            return;
        }

        // 광고 이벤트가 세션 생성 이전일 경우 거부
        if (rewardTime.isBefore(
                session.getCreatedAt().minusSeconds(120))) {
            log.warn("[AdMob SSV] Reward predates session");
            return;
        }

        // 7. 동일 transaction_id 중복 사용 차단
        if (sessionRepository.existsByAdmobTransactionId(
                callback.transactionId())) {
            log.warn("[AdMob SSV] Duplicate transaction");
            return;
        }

        // 8. 검증 완료 처리
        session.verify(callback.transactionId(), now);

        log.info(
                "[AdMob SSV] Reward verified. sessionId={}",
                session.getId()
        );
    }
}
