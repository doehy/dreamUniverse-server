package com.dreamuniverse_server.dream.dto.response;

import com.dreamuniverse_server.dream.entity.DreamElement;

public record DreamElementResponse(
        String type,
        String value
) {

    public static DreamElementResponse from(
            DreamElement element
    ) {
        return new DreamElementResponse(
                element.getElementType().name(),
                element.getElementValue()
        );
    }
}