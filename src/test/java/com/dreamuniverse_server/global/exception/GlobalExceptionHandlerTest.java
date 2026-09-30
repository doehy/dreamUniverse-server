package com.dreamuniverse_server.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test void preservesAuthenticationAndProviderStatusesWithoutExposingReasons() {
        var handler = new GlobalExceptionHandler();
        for (var status : new HttpStatus[]{HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.CONFLICT, HttpStatus.SERVICE_UNAVAILABLE}) {
            var response = handler.handleResponseStatusException(new ResponseStatusException(status, "private provider detail"));
            assertEquals(status.value(), response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().message().contains("private provider detail"));
        }
    }
}
