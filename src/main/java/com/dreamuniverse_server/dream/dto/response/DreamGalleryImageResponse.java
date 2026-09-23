package com.dreamuniverse_server.dream.dto.response;

import java.time.LocalDate;

public record DreamGalleryImageResponse(
        Long dreamId,
        LocalDate dreamDate,
        String imageUrl
) {
}