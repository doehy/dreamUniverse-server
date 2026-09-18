package com.dreamuniverse_server.dream.dto.response;

public record DreamAiUsageResponse(
        int remainingCount,
        int dailyLimit
) {
}