package com.dreamuniverse_server.auth.config;

import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TokenValidators {

    // JWT sub is a string; accept only canonical positive IDs within the signed BIGINT range.
    static boolean isUserId(String subject) {
        if (subject == null || !subject.matches("[1-9][0-9]{0,18}")) return false;
        try {
            return Long.parseLong(subject) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static OAuth2TokenValidator<Jwt> forAudienceAndIssuers(String audience, Set<String> issuers) {
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            boolean valid = issuer != null && issuers.contains(issuer)
                    && jwt.getAudience() != null && jwt.getAudience().contains(audience)
                    && jwt.getExpiresAt() != null && jwt.getIssuedAt() != null
                    && !jwt.getIssuedAt().isAfter(java.time.Instant.now().plusSeconds(60))
                    && jwt.getSubject() != null && !jwt.getSubject().isBlank();
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        });
    }
}
