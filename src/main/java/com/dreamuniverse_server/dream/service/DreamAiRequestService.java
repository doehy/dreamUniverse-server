
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 상태 확인 → 사용량 획득 → PROCESSING 전환을 하나의 트랜잭션으로 처리
 */
@Service
@RequiredArgsConstructor
public class DreamAiRequestService {

    private final DreamRepository dreamRepository;
    private final DreamAiUsageService dreamAiUsageService;

    @Transactional
    public LocalDate prepare(Long userId, User user, Long dreamId) {

        // 1. 해당 꿈 행에 대한 잠금 획득
        Dream dream = dreamRepository
                .findByIdAndUserIdForUpdate(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.DREAM_NOT_FOUND)
                );

        // 2. 이미 분석 중이면 중복 요청 차단
        if (dream.getAiStatus() == DreamAiStatus.PROCESSING) {
            throw new CustomException(
                    ErrorCode.DREAM_AI_ALREADY_PROCESSING
            );
        }

        // 3. AI 사용량 차감
        // 현재 트랜잭션에 참여하므로 상태 전환 실패 시 함께 롤백된다.
        LocalDate usageDate = dreamAiUsageService.acquire(user);

        // 4. 최초 분석, 재분석 모두 허용
        dream.startAiCompletion();

        return usageDate;
    }
}
