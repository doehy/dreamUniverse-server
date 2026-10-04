package com.dreamuniverse_server.dream.statistics.dto.response;

public record DreamProfileResponse(
        DreamTypeResponse dreamType,
        DreamDnaResponse dna
) {
}