package com.dreamuniverse_server.dream.dto.response;

import java.util.List;

public record DreamWorldResponse(
        List<DreamWorldElementResponse> people,
        List<DreamWorldElementResponse> places,
        List<DreamWorldElementResponse> symbols
) {
}