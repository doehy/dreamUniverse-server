package com.dreamuniverse_server.dream.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DreamWorldRandomDreamResponse(
        Long dreamId,
        LocalDate dreamDate,
        String title,
        List<DreamDetailImageResponse> images
) {
}