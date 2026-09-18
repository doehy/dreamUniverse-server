package com.dreamuniverse_server.dream.dto.response;

import com.dreamuniverse_server.dream.entity.DreamAnalysis;
import com.dreamuniverse_server.dream.entity.DreamEmotion;
import com.dreamuniverse_server.dream.entity.DreamElement;

import java.util.List;

public record DreamAnalysisResponse(
        String interpretation,
        List<DreamEmotionResponse> emotions,
        List<DreamElementResponse> elements
) {

    public static DreamAnalysisResponse from(
            DreamAnalysis analysis,
            List<DreamEmotion> emotions,
            List<DreamElement> elements
    ) {
        return new DreamAnalysisResponse(
                analysis.getInterpretation(),
                emotions.stream()
                        .map(DreamEmotionResponse::from)
                        .toList(),
                elements.stream()
                        .map(DreamElementResponse::from)
                        .toList()
        );
    }
}