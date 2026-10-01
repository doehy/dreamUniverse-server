package com.dreamuniverse_server.dream.dto.response;

import java.util.List;

public record DreamWorldRandomResponse(
        List<DreamWorldRandomElementResponse> elements
) {
}