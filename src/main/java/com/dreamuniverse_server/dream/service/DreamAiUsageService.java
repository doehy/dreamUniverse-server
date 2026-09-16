package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import com.dreamuniverse_server.dream.repository.DreamAiUsageRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DreamAiUsageService {

    private static final int FREE_DAILY_LIMIT = 2;

    private final DreamAiUsageRepository dreamAiUsageRepository;

    /**
     * AI 사용권 1회를 원자적으로 선점한다.
     */
    @Transactional
    public void acquire(Long userId) {

        LocalDate today = LocalDate.now();

        // 1. 오늘 row가 없으면 생성
        dreamAiUsageRepository.insertIfAbsent(
                userId,
                today
        );

        // 2. 한도 미만일 경우에만 +1
        int updatedRows =
                dreamAiUsageRepository.incrementIfAvailable(
                        userId,
                        today,
                        FREE_DAILY_LIMIT
                );

        // UPDATE 된 row가 없다는 것은 이미 한도에 도달했다는 뜻
        if (updatedRows == 0) {
            throw new CustomException(
                    ErrorCode.DREAM_AI_DAILY_LIMIT_EXCEEDED
            );
        }
    }

    /**
     * AI 처리 실패 시 선점했던 횟수를 되돌린다.
     */
    @Transactional
    public void release(Long userId) {

        dreamAiUsageRepository.decrement(
                userId,
                LocalDate.now()
        );
    }

    /**
     * 오늘 남은 AI 사용 횟수 조회
     */
    @Transactional(readOnly = true)
    public int getRemainingCount(Long userId) {

        int usedCount = dreamAiUsageRepository
                .findByUserIdAndUsageDate(
                        userId,
                        LocalDate.now()
                )
                .map(DreamAiUsage::getUsedCount)
                .orElse(0);

        return Math.max(
                FREE_DAILY_LIMIT - usedCount,
                0
        );
    }
}