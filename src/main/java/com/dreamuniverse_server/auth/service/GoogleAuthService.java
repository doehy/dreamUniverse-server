package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.dto.response.UserInfo;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {
    @Qualifier("googleJwtDecoder")
    private final JwtDecoder jwtDecoder;

    public UserInfo verify(String idToken) {
        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
                    || jwt.getClaimAsString("email") == null || jwt.getClaimAsString("email").isBlank()) {
                throw new BadJwtException("Verified email required");
            }
            return new UserInfo(OAuthProvider.GOOGLE, jwt.getSubject(), jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("name"));
        } catch (BadJwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
        } catch (JwtException e) {
            // Public-key retrieval/provider failures must not be reported as invalid credentials.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google authentication temporarily unavailable");
        }
    }
}
