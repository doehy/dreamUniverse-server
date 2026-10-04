package com.dreamuniverse_server.dream.statistics.dto.response;

import com.dreamuniverse_server.dream.statistics.type.DreamType;

public record DreamTypeResponse(
        String type,
        String name,
        String description
) {

    public static DreamTypeResponse from(
            DreamType dreamType
    ) {
        return new DreamTypeResponse(
                dreamType.name(),
                dreamType.getDisplayName(),
                dreamType.getDescription()
        );
    }
}