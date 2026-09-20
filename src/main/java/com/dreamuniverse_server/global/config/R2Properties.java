package com.dreamuniverse_server.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "r2")
public record R2Properties(

        @NotBlank
        String accountId,

        @NotBlank
        String accessKeyId,

        @NotBlank
        String secretAccessKey,

        @NotBlank
        String bucketName

) {

    public URI endpoint() {
        return URI.create(
                "https://%s.r2.cloudflarestorage.com"
                        .formatted(accountId)
        );
    }
}