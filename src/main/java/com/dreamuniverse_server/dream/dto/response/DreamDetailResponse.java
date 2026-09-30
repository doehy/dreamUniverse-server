package com.dreamuniverse_server.dream.dto.response;

import java.util.List;

public record DreamDetailResponse(
        DreamResponse dream,
        List<DreamDetailImageResponse> images,
        DreamAnalysisResponse analysis
) {
}