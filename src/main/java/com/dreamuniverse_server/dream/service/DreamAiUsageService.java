package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import com.dreamuniverse_server.dream.repository.DreamAiUsageRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DreamAiUsageService {

    public static final int FREE_DAILY_LIMIT = 1;

    private final DreamAiUsageRepository dreamAiUsageRepository;


    /**
     * AI 사용권 1회를 선점한다.
     *
     * 성공하면 실제로 사용한 날짜를 반환한다.
     */
    @Transactional
    public LocalDate acquire(User user) {

        LocalDate usageDate =
                getCurrentDate(user);

        /*
         * 오늘 row가 없으면 생성.
         * UNIQUE(user_id, usage_date)
         * +
         * ON DUPLICATE KEY
         * 로 최초 동시 요청도 처리.
         */
        dreamAiUsageRepository.insertIfAbsent(
                user.getId(),
                usageDate
        );

        /*
         * used_count < limit인 경우에만
         * DB에서 원자적으로 +1
         */
        int updatedRows =
                dreamAiUsageRepository.incrementIfAvailable(
                        user.getId(),
                        usageDate,
                        FREE_DAILY_LIMIT
                );

        if (updatedRows == 0) {
            throw new CustomException(
                    ErrorCode.DREAM_AI_DAILY_LIMIT_EXCEEDED
            );
        }

        return usageDate;
    }


    /**
     * AI 처리 실패 시
     * 이미 선점했던 사용 횟수를 반환한다.
     */
    @Transactional
    public void release(
            Long userId,
            LocalDate usageDate
    ) {
        dreamAiUsageRepository.decrement(
                userId,
                usageDate
        );
    }


    /**
     * 사용자의 현지 날짜 기준 남은 횟수 조회
     */
    @Transactional(readOnly = true)
    public int getRemainingCount(User user) {

        LocalDate usageDate =
                getCurrentDate(user);

        int usedCount =
                dreamAiUsageRepository
                        .findByUserIdAndUsageDate(
                                user.getId(),
                                usageDate
                        )
                        .map(DreamAiUsage::getUsedCount)
                        .orElse(0);

        return Math.max(
                FREE_DAILY_LIMIT - usedCount,
                0
        );
    }


    private LocalDate getCurrentDate(User user) {

        String timezone = user.getTimezone();

        if (timezone == null || timezone.isBlank()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }

        try {

            ZoneId zoneId =
                    ZoneId.of(timezone);

            return LocalDate.now(zoneId);

        } catch (DateTimeException e) {

            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }
    }
}