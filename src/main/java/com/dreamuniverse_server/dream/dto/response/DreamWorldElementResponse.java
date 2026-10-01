package com.dreamuniverse_server.dream.dto.response;

public record DreamWorldElementResponse(
        String name,
        long count
) {

    public static DreamWorldElementResponse of(
            String name,
            long count
    ) {
        return new DreamWorldElementResponse(
                name,
                count
        );
    }
}