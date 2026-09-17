package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.config.AuthProperties;
import com.dreamuniverse_server.auth.dto.request.SignupRequest;
import com.dreamuniverse_server.auth.dto.response.*;
import com.dreamuniverse_server.auth.repository.SignupSessionRepository;
import com.dreamuniverse_server.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SignupSessionRepository signupSessionRepository;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;

    public LoginResponse begin(UserInfo identity) {
        var userId = userService.findRegisteredUserId(identity.provider(), identity.providerUserId());
        if (userId.isPresent()) {
            return LoginResponse.loggedIn(refreshTokenService.create(userId.get()));
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long ttl = authProperties.token().signupTtlSeconds();
        signupSessionRepository.upsert(identity.provider().name(), identity.providerUserId(), hash(rawToken),
                identity.email(), identity.name(), Instant.now().plusSeconds(ttl));
        return LoginResponse.signupRequired(rawToken, ttl, identity.email(), identity.name());
    }

    public TokenResponse complete(SignupRequest request) {
        var session = signupSessionRepository.findLockedByTokenHash(hash(request.signupToken()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signup token"));
        Instant now = Instant.now();
        if (!session.isActive(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired or consumed signup token");
        }
        // Email and provider identity come exclusively from the verified server-side signup session.
        Long userId = userService.completeSignup(session.getProvider(), session.getProviderUserId(),
                session.getEmail(), session.getName(), request.nickname(), request.birthDate(),
                request.termsOfServiceAgreed(), request.privacyPolicyAgreed(), request.timezone());
        session.consume(now);
        // Discard any sessions issued by the former automatic-registration flow.
        refreshTokenService.revokeAllForUser(userId);
        return refreshTokenService.create(userId);
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
