package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.dto.response.TokenResponse;
import com.dreamuniverse_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final GoogleAuthService googleAuthService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public TokenResponse login(String idToken) {
        var identity = googleAuthService.verify(idToken);
        Long userId = userService.registerOAuthUser(
                identity.provider(), identity.providerUserId(), identity.email(), identity.name());
        return refreshTokenService.create(userId);
    }
}
