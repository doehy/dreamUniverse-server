package com.dreamuniverse_server.dream.dto.ai;

import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.entity.DreamEmotionType;

import java.util.List;

public record DreamAnalysisResult(
        String summary,
        String interpretation,
        List<EmotionResult> emotions,
        List<ElementResult> elements
) {

    public record EmotionResult(DreamEmotionType type, int score) { }

    public record ElementResult(DreamElementType type, String value) { }
}