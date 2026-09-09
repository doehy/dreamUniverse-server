package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.config.AuthProperties;
import com.dreamuniverse_server.auth.dto.response.TokenResponse;
import com.dreamuniverse_server.auth.entity.*;
import com.dreamuniverse_server.auth.exception.InvalidRefreshTokenException;
import com.dreamuniverse_server.auth.repository.*;
import com.dreamuniverse_server.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshSessionRepository refreshSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenService accessTokenService;
    private final AuthProperties authProperties;
    private final UserService userService;

    public void revokeAllForUser(Long userId) {
        refreshSessionRepository.revokeAllByUserId(userId, Instant.now());
    }

    public TokenResponse create(Long userId) {
        userService.getProfile(userId);
        Instant now = Instant.now();
        var session = refreshSessionRepository.save(RefreshSession.create(userId,
                now.plusSeconds(authProperties.token().refreshTtlSeconds())));
        return issuePair(session, now);
    }

    // Commit session revocation even when rejecting a replay with 401.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenResponse refresh(String rawToken) {
        String hash = hash(rawToken);
        Long sessionId = refreshTokenRepository.findSessionId(hash).orElseThrow(InvalidRefreshTokenException::new);
        var session = refreshSessionRepository.findLockedById(sessionId).orElseThrow(InvalidRefreshTokenException::new);
        var token = refreshTokenRepository.findLockedByHash(hash).orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();
        if (!session.isActive(now)) throw new InvalidRefreshTokenException();
        if (token.getConsumedAt() != null) {
            session.revoke(now);
            throw new InvalidRefreshTokenException();
        }
        userService.getProfile(session.getUserId());
        token.consume(now);
        return issuePair(session, now);
    }

    public void logout(String rawToken) {
        refreshTokenRepository.findSessionId(hash(rawToken)).flatMap(refreshSessionRepository::findLockedById)
                .ifPresent(session -> session.revoke(Instant.now()));
    }

    private TokenResponse issuePair(RefreshSession session, Instant now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(RefreshToken.create(hash(rawToken), session.getId()));
        return new TokenResponse(accessTokenService.issue(session.getUserId()), "Bearer",
                authProperties.token().ttlSeconds(), rawToken,
                Math.max(0, Duration.between(now, session.getExpiresAt()).getSeconds()));
    }

    private static String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
