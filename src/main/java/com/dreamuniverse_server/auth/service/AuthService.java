package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final GoogleAuthService googleAuthService;
    private final SignupService signupService;

    public LoginResponse login(String idToken) {
        return signupService.begin(googleAuthService.verify(idToken));
    }
}
