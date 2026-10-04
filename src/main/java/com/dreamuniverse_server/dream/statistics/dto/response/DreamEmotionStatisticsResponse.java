package com.dreamuniverse_server.dream.statistics.dto.response;

public record DreamEmotionStatisticsResponse(
        String emotion,
        long score,
        double ratio
) {
}