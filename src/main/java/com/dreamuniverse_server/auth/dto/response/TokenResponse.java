package com.dreamuniverse_server.auth.dto.response;

public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                            String refreshToken, long refreshExpiresIn) {
    @Override
    public String toString() { return "TokenResponse[REDACTED]"; }
}
