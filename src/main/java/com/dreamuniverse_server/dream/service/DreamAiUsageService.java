package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import com.dreamuniverse_server.dream.repository.DreamAiUsageRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class DreamAiUsageService {

    private static final int FREE_DAILY_LIMIT = 2;

    private final DreamAiUsageRepository dreamAiUsageRepository;

    /**
     * 오늘 AI를 더 사용할 수 있는지 확인
     */
    @Transactional(readOnly = true)
    public void validateAvailable(Long userId) {

        int usedCount = dreamAiUsageRepository
                .findByUserIdAndUsageDate(
                        userId,
                        LocalDate.now()
                )
                .map(DreamAiUsage::getUsedCount)
                .orElse(0);

        if (usedCount >= FREE_DAILY_LIMIT) {
            throw new CustomException(
                    ErrorCode.DREAM_AI_DAILY_LIMIT_EXCEEDED
            );
        }
    }

    /**
     * AI 작업이 모두 성공한 뒤 사용 횟수 1 증가
     */
    public void use(User user) {

        LocalDate today = LocalDate.now();

        DreamAiUsage usage = dreamAiUsageRepository
                .findByUserIdAndUsageDate(
                        user.getId(),
                        today
                )
                .orElseGet(() ->
                        dreamAiUsageRepository.save(
                                DreamAiUsage.create(
                                        user,
                                        today
                                )
                        )
                );

        usage.increase();
    }

    /**
     * 프론트에서 "AI로 완성하기 (2/2)" 같은 UI를 보여줄 때 사용
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