package com.dreamuniverse_server.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(@NotNull @Valid Google google, @NotNull @Valid Token token) {
    public record Google(@NotBlank String clientId) {}
    public record Token(@NotBlank String secret, @DefaultValue("900") @Positive long ttlSeconds,
                        @DefaultValue("2592000") @Positive long refreshTtlSeconds) {}
}
