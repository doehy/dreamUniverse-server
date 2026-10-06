package com.dreamuniverse_server.dream.statistics.dto.response;

import com.dreamuniverse_server.dream.statistics.type.DreamType;

public record DreamTypeResponse(
        String type,
        String name,
        String description,
        String imageUrl
) {

    public static DreamTypeResponse from(
            DreamType dreamType,
            String imageBaseUrl
    ) {
        return new DreamTypeResponse(
                dreamType.name(),
                dreamType.getDisplayName(),
                dreamType.getDescription(),
                createImageUrl(
                        imageBaseUrl,
                        dreamType.getImageKey()
                )
        );
    }

    private static String createImageUrl(
            String imageBaseUrl,
            String imageKey
    ) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }

        String normalizedBaseUrl =
                imageBaseUrl.endsWith("/")
                        ? imageBaseUrl.substring(
                        0,
                        imageBaseUrl.length() - 1
                )
                        : imageBaseUrl;

        return normalizedBaseUrl + "/" + imageKey;
    }
}