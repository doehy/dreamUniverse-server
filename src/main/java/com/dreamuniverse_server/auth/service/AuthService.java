package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;
    private final SignupService signupService;

    public LoginResponse loginGoogle(String idToken) {
        return signupService.begin(googleAuthService.verify(idToken));
    }

    public LoginResponse loginKakao(String accessToken) {
        return signupService.begin(kakaoAuthService.verify(accessToken));
    }
}
