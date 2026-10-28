package com.dreamuniverse_server.ad.service;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import com.dreamuniverse_server.ad.entity.RewardedAdStatus;
import com.dreamuniverse_server.ad.repository.RewardedAdSessionRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RewardedAdSessionService {

    private final RewardedAdSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public RewardedAdSession create(Long userId) {

        User user = userRepository.findByIdForUpdate(userId)
                .filter(User::isSignupCompleted)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        return sessionRepository.save(
                RewardedAdSession.create(
                        user.getId(),
                        Instant.now()
                )
        );
    }

    @Transactional(readOnly = true)
    public RewardedAdSession get(
            Long userId,
            String sessionId
    ) {
        return sessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.AD_REWARD_NOT_AVAILABLE
                        )
                );
    }

    /**
     * 검증된 광고 보상을 정확히 한 번 소비한다.
     *
     * AI 사용량 차감 및 PROCESSING 전환과
     * 반드시 같은 트랜잭션에서 실행되어야 한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeVerifiedReward(
            Long userId,
            String sessionId
    ) {

        if (sessionId == null || sessionId.isBlank()) {
            throw new CustomException(
                    ErrorCode.AD_REWARD_REQUIRED
            );
        }

        RewardedAdSession session = sessionRepository
                .findByIdAndUserIdForUpdate(
                        sessionId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.AD_REWARD_NOT_AVAILABLE
                        )
                );

        if (session.getStatus() != RewardedAdStatus.VERIFIED) {
            throw new CustomException(
                    ErrorCode.AD_REWARD_NOT_AVAILABLE
            );
        }

        Instant now = Instant.now();

        if (session.getRewardExpiresAt() == null
                || !now.isBefore(session.getRewardExpiresAt())) {
            throw new CustomException(
                    ErrorCode.AD_REWARD_NOT_AVAILABLE
            );
        }

        session.consume(now);
    }
}
