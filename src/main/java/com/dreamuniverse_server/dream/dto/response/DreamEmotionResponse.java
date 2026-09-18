package com.dreamuniverse_server.dream.dto.response;

import com.dreamuniverse_server.dream.entity.DreamEmotion;

public record DreamEmotionResponse(
        String type,
        int score
) {

    public static DreamEmotionResponse from(
            DreamEmotion emotion
    ) {
        return new DreamEmotionResponse(
                emotion.getEmotionType().name(),
                emotion.getScore()
        );
    }
}