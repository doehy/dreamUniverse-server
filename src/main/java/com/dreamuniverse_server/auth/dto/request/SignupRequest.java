package com.dreamuniverse_server.auth.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SignupRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String signupToken,
        @NotBlank @Size(max = 30) String nickname,
        @NotNull @PastOrPresent LocalDate birthDate,
        @NotNull @AssertTrue Boolean termsOfServiceAgreed,
        @NotNull @AssertTrue Boolean privacyPolicyAgreed) {
    public SignupRequest {
        if (nickname != null) nickname = nickname.strip();
    }

    @Override
    public String toString() { return "SignupRequest[REDACTED]"; }
}
