package com.dreamuniverse_server.ad.service;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import com.dreamuniverse_server.ad.repository.RewardedAdSessionRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RewardedAdSessionService {

    private final RewardedAdSessionRepository sessionRepository;
    private final UserRepository userRepository;

    /**
     * 로그인한 사용자의 광고 세션 생성
     */
    @Transactional
    public RewardedAdSession create(Long userId) {

        User user = userRepository.findById(userId)
                .filter(User::isSignupCompleted)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        RewardedAdSession session =
                RewardedAdSession.create(
                        user.getId(),
                        Instant.now()
                );

        return sessionRepository.save(session);
    }

    /**
     * 본인 광고 세션 상태 조회
     */
    @Transactional(readOnly = true)
    public RewardedAdSession get(
            Long userId,
            String sessionId
    ) {
        return sessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.INVALID_INPUT_VALUE)
                );
    }
}
