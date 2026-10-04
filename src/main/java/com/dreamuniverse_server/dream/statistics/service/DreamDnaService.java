package com.dreamuniverse_server.dream.statistics.service;

import com.dreamuniverse_server.dream.statistics.dto.response.DreamDnaResponse;
import com.dreamuniverse_server.dream.statistics.repository.DreamDnaRepository;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaAiScoreProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaElementProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaEmotionProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaExperienceProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamDnaRecurrenceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DreamDnaService {

    private final DreamDnaRepository dreamDnaRepository;

    public DreamDnaResponse calculate(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        DreamDnaExperienceProjection experience =
                dreamDnaRepository.findExperienceScores(
                        userId,
                        startDate,
                        endDate
                );

        DreamDnaEmotionProjection emotion =
                dreamDnaRepository.findEmotionScores(
                        userId,
                        startDate,
                        endDate
                );

        DreamDnaAiScoreProjection aiScores =
                dreamDnaRepository.findAiScores(
                        userId,
                        startDate,
                        endDate
                );

        DreamDnaRecurrenceProjection recurrence =
                dreamDnaRepository.findRecurrenceScores(
                        userId,
                        startDate,
                        endDate
                );

        DreamDnaElementProjection elements =
                dreamDnaRepository.findElementScores(
                        userId,
                        startDate,
                        endDate
                );

        long dreamCount =
                valueOrZero(
                        experience.getDreamCount()
                );

        double nightmareRatio =
                ratio(
                        valueOrZero(
                                experience.getNightmareCount()
                        ),
                        dreamCount
                );

        double sleepParalysisRatio =
                ratio(
                        valueOrZero(
                                experience.getSleepParalysisCount()
                        ),
                        dreamCount
                );

        double lucidDreamRatio =
                ratio(
                        valueOrZero(
                                experience.getLucidDreamCount()
                        ),
                        dreamCount
                );

        int emotionIntensity =
                score(
                        valueOrZero(
                                emotion.getAverageEmotionPeak()
                        )
                );

        int tension =
                calculateTension(
                        valueOrZero(
                                emotion.getAverageNegativeEmotion()
                        ),
                        nightmareRatio,
                        sleepParalysisRatio
                );

        int lucidity =
                calculateLucidity(
                        lucidDreamRatio
                );

        int recurrenceScore =
                calculateRecurrence(
                        valueOrZero(
                                recurrence.getRepeatedAppearanceCount()
                        ),
                        valueOrZero(
                                recurrence.getTotalAppearanceCount()
                        )
                );

        int relationship =
                calculateRelationship(
                        valueOrZero(
                                elements.getPersonElementCount()
                        ),
                        valueOrZero(
                                elements.getTotalElementCount()
                        )
                );

        int symbolism =
                score(
                        valueOrZero(
                                aiScores.getAverageSymbolismScore()
                        )
                );

        int dynamism =
                score(
                        valueOrZero(
                                aiScores.getAverageDynamismScore()
                        )
                );

        int fantasy =
                score(
                        valueOrZero(
                                aiScores.getAverageFantasyScore()
                        )
                );

        return new DreamDnaResponse(
                emotionIntensity,
                tension,
                lucidity,
                recurrenceScore,
                relationship,
                symbolism,
                dynamism,
                fantasy
        );
    }

    private int calculateTension(
            double averageNegativeEmotion,
            double nightmareRatio,
            double sleepParalysisRatio
    ) {
        double result =
                averageNegativeEmotion * 0.5
                        + nightmareRatio * 100.0 * 0.3
                        + sleepParalysisRatio * 100.0 * 0.2;

        return score(result);
    }

    private int calculateLucidity(
            double lucidDreamRatio
    ) {
        double result =
                Math.sqrt(
                        lucidDreamRatio
                ) * 100.0;

        return score(result);
    }

    private int calculateRecurrence(
            long repeatedAppearanceCount,
            long totalAppearanceCount
    ) {
        if (totalAppearanceCount == 0) {
            return 0;
        }

        double result =
                repeatedAppearanceCount
                        * 100.0
                        / totalAppearanceCount;

        return score(result);
    }

    private int calculateRelationship(
            long personElementCount,
            long totalElementCount
    ) {
        if (totalElementCount == 0) {
            return 0;
        }

        double result =
                personElementCount
                        * 100.0
                        / totalElementCount;

        return score(result);
    }

    private double ratio(
            long value,
            long total
    ) {
        if (total == 0) {
            return 0.0;
        }

        return (double) value / total;
    }

    private long valueOrZero(
            Long value
    ) {
        return value == null
                ? 0L
                : value;
    }

    private double valueOrZero(
            Double value
    ) {
        return value == null
                ? 0.0
                : value;
    }

    private int score(
            double value
    ) {
        int rounded =
                (int) Math.round(value);

        return Math.max(
                0,
                Math.min(
                        100,
                        rounded
                )
        );
    }
}