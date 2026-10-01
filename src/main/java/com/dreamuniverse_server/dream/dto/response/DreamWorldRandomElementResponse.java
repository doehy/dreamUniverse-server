package com.dreamuniverse_server.dream.dto.response;

import com.dreamuniverse_server.dream.entity.DreamElementType;

public record DreamWorldRandomElementResponse(
        DreamElementType type,
        String name,
        long count
) {

    public static DreamWorldRandomElementResponse of(
            DreamElementType type,
            String name,
            long count
    ) {
        return new DreamWorldRandomElementResponse(
                type,
                name,
                count
        );
    }
}