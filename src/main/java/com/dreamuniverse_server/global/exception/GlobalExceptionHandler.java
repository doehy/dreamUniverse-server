package com.dreamuniverse_server.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            CustomException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn(
                "[{} {}] {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                errorCode.getCode(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e
    ) {
        Map<String, String> validation = new HashMap<>();

        e.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        validation.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, validation));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        int status = e.getStatusCode().value();
        // Preserve authentication/service errors without exposing provider details or credentials.
        String message = switch (status) {
            case 401 -> "인증이 만료되었거나 유효하지 않습니다.";
            case 403 -> "허용되지 않은 요청입니다.";
            case 409 -> "요청이 현재 상태와 충돌합니다.";
            case 503 -> "외부 인증 서비스 또는 서버 설정을 확인해 주세요.";
            default -> "요청을 처리하지 못했습니다.";
        };
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse("HTTP_" + status, message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception [{} {}]",
                request.getMethod(),
                request.getRequestURI(),
                e
        );

        ErrorCode errorCode =
                ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }
}