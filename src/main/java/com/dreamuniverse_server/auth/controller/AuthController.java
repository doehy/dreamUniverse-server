package com.dreamuniverse_server.auth.controller;

import com.dreamuniverse_server.auth.dto.request.*;
import com.dreamuniverse_server.auth.dto.response.TokenResponse;
import com.dreamuniverse_server.auth.dto.response.LoginResponse;
import com.dreamuniverse_server.auth.service.*;
import com.dreamuniverse_server.user.dto.UserProfile;
import com.dreamuniverse_server.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SignupService signupService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    /** Android에서 받은 Google ID 토큰으로 로그인하고 서비스 토큰 쌍을 발급합니다. */
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(authService.login(request.idToken()));
    }

    /** 신규 사용자의 필수 정보를 저장하고 가입용 토큰을 한 번만 소비합니다. */
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(signupService.complete(request));
    }

    /** 기존 리프레시 토큰을 소비하고 새 액세스·리프레시 토큰을 발급합니다. */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(refreshTokenService.refresh(request.refreshToken()));
    }

    /** 해당 로그인 세션의 리프레시 토큰을 폐기합니다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    /** 액세스 토큰의 내부 회원 ID로 현재 회원 정보를 DB에서 조회합니다. */
    @GetMapping("/me")
    public ResponseEntity<UserProfile> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(userService.getProfile(Long.valueOf(jwt.getSubject())));
    }
}
