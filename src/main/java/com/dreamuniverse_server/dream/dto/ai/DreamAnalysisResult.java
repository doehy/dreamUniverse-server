package com.dreamuniverse_server.dream.dto.ai;

import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.entity.DreamEmotionType;
import lombok.Getter;

import java.util.List;

@Getter
public class DreamAnalysisResult {

    private String interpretation;

    private List<EmotionResult> emotions;

    private List<ElementResult> elements;

    private int symbolismScore;

    private int dynamismScore;

    private int fantasyScore;

    @Getter
    public static class EmotionResult {
        private DreamEmotionType type;
        private int score;
    }

    @Getter
    public static class ElementResult {
        private DreamElementType type;
        private String value;
    }
}