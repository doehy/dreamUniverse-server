package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import com.dreamuniverse_server.dream.policy.DreamUsagePolicy;
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

    private final DreamAiUsageRepository dreamAiUsageRepository;
    private final DreamEntitlementService dreamEntitlementService;


    @Transactional
    public LocalDate acquire(User user) {

        LocalDate usageDate =
                getCurrentDate(user);

        DreamUsagePolicy policy =
                dreamEntitlementService.getPolicy(
                        user.getId()
                );

        dreamAiUsageRepository.insertIfAbsent(
                user.getId(),
                usageDate
        );

        int updatedRows =
                dreamAiUsageRepository.incrementIfAvailable(
                        user.getId(),
                        usageDate,
                        policy.dailyAiLimit()
                );

        if (updatedRows == 0) {
            throw new CustomException(
                    ErrorCode.DREAM_AI_DAILY_LIMIT_EXCEEDED
            );
        }

        return usageDate;
    }


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


    @Transactional(readOnly = true)
    public int getRemainingCount(User user) {

        LocalDate usageDate =
                getCurrentDate(user);

        DreamUsagePolicy policy =
                dreamEntitlementService.getPolicy(
                        user.getId()
                );

        int usedCount =
                dreamAiUsageRepository
                        .findByUserIdAndUsageDate(
                                user.getId(),
                                usageDate
                        )
                        .map(DreamAiUsage::getUsedCount)
                        .orElse(0);

        return Math.max(
                policy.dailyAiLimit() - usedCount,
                0
        );
    }


    @Transactional(readOnly = true)
    public int getDailyLimit(User user) {

        return dreamEntitlementService
                .getPolicy(user.getId())
                .dailyAiLimit();
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