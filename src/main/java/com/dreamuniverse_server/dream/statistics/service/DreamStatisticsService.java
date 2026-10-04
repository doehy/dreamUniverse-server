package com.dreamuniverse_server.dream.statistics.service;

import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamEmotionStatisticsResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamExperienceStatisticsResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamMonthlyEmotionTrendResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamProfileResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamRecentStatisticsResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamStatisticsElementResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamYearlyStatisticsResponse;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamEmotionScoreProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamExperienceStatisticsProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamMonthlyEmotionScoreProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamStatisticsElementProjection;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DreamStatisticsService {

    private static final int TOP_ELEMENT_COUNT = 5;

    private final DreamRepository dreamRepository;
    private final DreamEmotionRepository dreamEmotionRepository;
    private final DreamElementRepository dreamElementRepository;
    private final UserRepository userRepository;
    private final DreamProfileService dreamProfileService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DreamRecentStatisticsResponse getRecentStatistics(
            Long userId
    ) {
        LocalDate endDate =
                getUserToday(userId);

        LocalDate startDate =
                endDate.minusDays(29);

        CommonStatistics statistics =
                getCommonStatistics(
                        userId,
                        startDate,
                        endDate
                );

        DreamProfileResponse dreamProfile =
                createDreamProfile(
                        userId,
                        startDate,
                        endDate,
                        statistics.dreamCount()
                );

        return new DreamRecentStatisticsResponse(
                startDate,
                endDate,
                statistics.dreamCount(),
                dreamProfile,
                statistics.sleepParalysis(),
                statistics.nightmare(),
                statistics.lucidDream(),
                statistics.dominantEmotion(),
                statistics.emotions(),
                statistics.topElements()
        );
    }

    @Transactional(readOnly = true)
    public DreamYearlyStatisticsResponse getYearlyStatistics(
            Long userId,
            int year
    ) {
        LocalDate today =
                getUserToday(userId);

        LocalDate startDate =
                LocalDate.of(
                        year,
                        1,
                        1
                );

        LocalDate endDate =
                LocalDate.of(
                        year,
                        12,
                        31
                );

        CommonStatistics statistics =
                getCommonStatistics(
                        userId,
                        startDate,
                        endDate
                );

        DreamProfileResponse dreamProfile =
                createDreamProfile(
                        userId,
                        startDate,
                        endDate,
                        statistics.dreamCount()
                );

        List<DreamMonthlyEmotionTrendResponse> monthlyTrend =
                getMonthlyTrend(
                        userId,
                        year,
                        startDate,
                        endDate,
                        today
                );

        return new DreamYearlyStatisticsResponse(
                year,
                statistics.dreamCount(),
                dreamProfile,
                statistics.sleepParalysis(),
                statistics.nightmare(),
                statistics.lucidDream(),
                statistics.dominantEmotion(),
                statistics.emotions(),
                statistics.topElements(),
                monthlyTrend
        );
    }

    private CommonStatistics getCommonStatistics(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        DreamExperienceStatisticsProjection experience =
                dreamRepository.findExperienceStatistics(
                        userId,
                        startDate,
                        endDate
                );

        long dreamCount =
                valueOrZero(
                        experience.getDreamCount()
                );

        DreamExperienceStatisticsResponse sleepParalysis =
                createExperienceResponse(
                        valueOrZero(
                                experience.getSleepParalysisCount()
                        ),
                        dreamCount
                );

        DreamExperienceStatisticsResponse nightmare =
                createExperienceResponse(
                        valueOrZero(
                                experience.getNightmareCount()
                        ),
                        dreamCount
                );

        DreamExperienceStatisticsResponse lucidDream =
                createExperienceResponse(
                        valueOrZero(
                                experience.getLucidDreamCount()
                        ),
                        dreamCount
                );

        List<DreamEmotionStatisticsResponse> emotions =
                createEmotionStatistics(
                        dreamEmotionRepository.findEmotionScores(
                                userId,
                                startDate,
                                endDate
                        )
                );

        String dominantEmotion =
                emotions.isEmpty()
                        ? null
                        : emotions.getFirst().emotion();

        List<DreamStatisticsElementResponse> topElements =
                dreamElementRepository
                        .findTopStatisticsElements(
                                userId,
                                startDate,
                                endDate,
                                PageRequest.of(
                                        0,
                                        TOP_ELEMENT_COUNT
                                )
                        )
                        .stream()
                        .map(this::toElementResponse)
                        .toList();

        return new CommonStatistics(
                dreamCount,
                sleepParalysis,
                nightmare,
                lucidDream,
                dominantEmotion,
                emotions,
                topElements
        );
    }

    private DreamProfileResponse createDreamProfile(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            long dreamCount
    ) {
        if (dreamCount == 0) {
            return null;
        }

        return dreamProfileService.getProfile(
                userId,
                startDate,
                endDate
        );
    }

    private List<DreamEmotionStatisticsResponse> createEmotionStatistics(
            List<DreamEmotionScoreProjection> projections
    ) {
        long totalScore =
                projections.stream()
                        .mapToLong(
                                projection ->
                                        valueOrZero(
                                                projection.getTotalScore()
                                        )
                        )
                        .sum();

        return projections.stream()
                .map(projection -> {
                    long score =
                            valueOrZero(
                                    projection.getTotalScore()
                            );

                    return new DreamEmotionStatisticsResponse(
                            projection.getEmotionType(),
                            score,
                            ratio(
                                    score,
                                    totalScore
                            )
                    );
                })
                .toList();
    }

    private List<DreamMonthlyEmotionTrendResponse> getMonthlyTrend(
            Long userId,
            int year,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today
    ) {
        List<DreamMonthlyEmotionScoreProjection> rows =
                dreamEmotionRepository
                        .findMonthlyEmotionScores(
                                userId,
                                startDate,
                                endDate
                        );

        Map<Integer, List<DreamMonthlyEmotionScoreProjection>> rowsByMonth =
                rows.stream()
                        .collect(
                                Collectors.groupingBy(
                                        DreamMonthlyEmotionScoreProjection::getMonth
                                )
                        );

        int lastMonth =
                year == today.getYear()
                        ? today.getMonthValue()
                        : 12;

        List<DreamMonthlyEmotionTrendResponse> result =
                new ArrayList<>();

        for (int month = 1; month <= lastMonth; month++) {
            List<DreamMonthlyEmotionScoreProjection> monthRows =
                    rowsByMonth.getOrDefault(
                            month,
                            List.of()
                    );

            long totalScore =
                    monthRows.stream()
                            .mapToLong(
                                    row ->
                                            valueOrZero(
                                                    row.getTotalScore()
                                            )
                            )
                            .sum();

            List<DreamEmotionStatisticsResponse> emotions =
                    monthRows.stream()
                            .map(row -> {
                                long score =
                                        valueOrZero(
                                                row.getTotalScore()
                                        );

                                return new DreamEmotionStatisticsResponse(
                                        row.getEmotionType(),
                                        score,
                                        ratio(
                                                score,
                                                totalScore
                                        )
                                );
                            })
                            .toList();

            String dominantEmotion =
                    emotions.isEmpty()
                            ? null
                            : emotions.getFirst().emotion();

            result.add(
                    new DreamMonthlyEmotionTrendResponse(
                            month,
                            dominantEmotion,
                            emotions
                    )
            );
        }

        return result;
    }

    private DreamExperienceStatisticsResponse createExperienceResponse(
            long count,
            long total
    ) {
        return new DreamExperienceStatisticsResponse(
                count,
                ratio(
                        count,
                        total
                )
        );
    }

    private DreamStatisticsElementResponse toElementResponse(
            DreamStatisticsElementProjection projection
    ) {
        return new DreamStatisticsElementResponse(
                DreamElementType.valueOf(
                        projection.getElementType()
                ),
                projection.getElementValue(),
                valueOrZero(
                        projection.getAppearanceCount()
                )
        );
    }

    private LocalDate getUserToday(
            Long userId
    ) {
        User user =
                userRepository.findById(userId)
                        .orElseThrow();

        ZoneId zoneId =
                ZoneId.of(
                        user.getTimezone()
                );

        return LocalDate.now(
                clock.withZone(zoneId)
        );
    }

    private double ratio(
            long value,
            long total
    ) {
        if (total == 0) {
            return 0.0;
        }

        return BigDecimal
                .valueOf(
                        value * 100.0 / total
                )
                .setScale(
                        1,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }

    private long valueOrZero(
            Long value
    ) {
        return value == null
                ? 0L
                : value;
    }

    private record CommonStatistics(
            long dreamCount,
            DreamExperienceStatisticsResponse sleepParalysis,
            DreamExperienceStatisticsResponse nightmare,
            DreamExperienceStatisticsResponse lucidDream,
            String dominantEmotion,
            List<DreamEmotionStatisticsResponse> emotions,
            List<DreamStatisticsElementResponse> topElements
    ) {
    }
}