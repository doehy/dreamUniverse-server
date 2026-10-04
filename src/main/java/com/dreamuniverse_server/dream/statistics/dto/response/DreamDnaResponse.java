package com.dreamuniverse_server.dream.statistics.dto.response;

public record DreamDnaResponse(
        int emotionIntensity,
        int tension,
        int lucidity,
        int recurrence,
        int relationship,
        int symbolism,
        int dynamism,
        int fantasy
) {
}