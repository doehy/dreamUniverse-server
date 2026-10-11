package com.dreamuniverse_server.dream.policy;

public record DreamUsagePolicy(
        int dailyAiLimit,
        int panelCount,
        boolean adRequired
) {
}