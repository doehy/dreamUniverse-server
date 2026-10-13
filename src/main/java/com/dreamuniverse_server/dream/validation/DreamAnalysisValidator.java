
package com.dreamuniverse_server.dream.validation;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult.ElementResult;
import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult.EmotionResult;
import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.entity.DreamEmotionType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DreamAnalysisValidator {

    public void validate(DreamAnalysisResult result) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "AI analysis result is null"
            );
        }

        if (result.getInterpretation() == null
                || result.getInterpretation().isBlank()) {
            throw new IllegalArgumentException(
                    "AI interpretation is empty"
            );
        }

        validateEmotions(result.getEmotions());
        validateElements(result.getElements());

        validateScore("symbolismScore", result.getSymbolismScore());
        validateScore("dynamismScore", result.getDynamismScore());
        validateScore("fantasyScore", result.getFantasyScore());
    }

    private void validateEmotions(List<EmotionResult> emotions) {
        if (emotions == null || emotions.isEmpty()) {
            throw new IllegalArgumentException(
                    "AI emotions are empty"
            );
        }

        Set<DreamEmotionType> seenTypes = new HashSet<>();
        int totalScore = 0;

        for (EmotionResult emotion : emotions) {
            if (emotion == null || emotion.getType() == null) {
                throw new IllegalArgumentException(
                        "AI emotion type is null"
                );
            }

            DreamEmotionType type = emotion.getType();

            if (!seenTypes.add(type)) {
                throw new IllegalArgumentException(
                        "Duplicate AI emotion type: " + type
                );
            }

            int score = emotion.getScore();

            validateScore("emotion." + type, score);
            totalScore += score;
        }

        if (totalScore != 100) {
            throw new IllegalArgumentException(
                    "AI emotion total score must be 100, actual: "
                            + totalScore
            );
        }
    }

    private void validateElements(List<ElementResult> elements) {
        if (elements == null) {
            throw new IllegalArgumentException(
                    "AI elements are null"
            );
        }

        Set<ElementKey> seenElements = new HashSet<>();

        for (ElementResult element : elements) {
            if (element == null || element.getType() == null) {
                throw new IllegalArgumentException(
                        "AI element type is null"
                );
            }

            String value = element.getValue();

            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "AI element value is empty"
                );
            }

            ElementKey key = new ElementKey(
                    element.getType(),
                    value.trim()
            );

            if (!seenElements.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate AI element: "
                                + key.type() + "/" + key.value()
                );
            }
        }
    }

    private void validateScore(String name, int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                    name + " must be between 0 and 100, actual: "
                            + score
            );
        }
    }

    private record ElementKey(
            DreamElementType type,
            String value
    ) {
    }
}
