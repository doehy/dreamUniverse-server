package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.config.AuthConfig;
import com.dreamuniverse_server.auth.config.AuthProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessTokenService {
    private final SecretKey secretKey;
    private final AuthProperties authProperties;

    public String issue(Long userId) {
        long ttlSeconds = authProperties.token().ttlSeconds();
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(AuthConfig.ISSUER).audience(AuthConfig.AUDIENCE)
                .subject(userId.toString()).jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(ttlSeconds))).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims);
        try {
            jwt.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to issue access token", e);
        }
        return jwt.serialize();
    }
}
