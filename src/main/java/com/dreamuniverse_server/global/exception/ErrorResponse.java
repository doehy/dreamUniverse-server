package com.dreamuniverse_server.global.exception;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, String> validation
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            Map<String, String> validation
    ) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                validation
        );
    }
}