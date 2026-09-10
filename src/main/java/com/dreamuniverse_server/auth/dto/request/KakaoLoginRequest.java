package com.dreamuniverse_server.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record KakaoLoginRequest(
        @NotBlank @Size(max = 4096) @Pattern(regexp = "[A-Za-z0-9._~+/-]+=*") String accessToken) {
    @Override
    public String toString() { return "KakaoLoginRequest[REDACTED]"; }
}
