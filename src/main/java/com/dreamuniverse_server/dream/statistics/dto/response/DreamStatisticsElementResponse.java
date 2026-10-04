package com.dreamuniverse_server.dream.statistics.dto.response;

import com.dreamuniverse_server.dream.entity.DreamElementType;

public record DreamStatisticsElementResponse(
        DreamElementType type,
        String name,
        long count
) {
}