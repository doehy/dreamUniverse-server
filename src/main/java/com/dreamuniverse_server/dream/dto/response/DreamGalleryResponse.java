package com.dreamuniverse_server.dream.dto.response;

import java.util.List;

public record DreamGalleryResponse(
        List<DreamGalleryImageResponse> images,
        Long nextCursor,
        boolean hasNext
) {
}