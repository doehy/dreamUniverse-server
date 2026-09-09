package com.dreamuniverse_server.auth.dto.response;

public record LoginResponse(Status status, TokenResponse tokens, SignupInfo signup) {
    public enum Status { LOGIN_SUCCESS, SIGNUP_REQUIRED }
    public record SignupInfo(String signupToken, long expiresIn, String email, String name) {
        @Override
        public String toString() { return "SignupInfo[REDACTED]"; }
    }

    public static LoginResponse loggedIn(TokenResponse tokens) {
        return new LoginResponse(Status.LOGIN_SUCCESS, tokens, null);
    }

    public static LoginResponse signupRequired(String token, long expiresIn, String email, String name) {
        return new LoginResponse(Status.SIGNUP_REQUIRED, null, new SignupInfo(token, expiresIn, email, name));
    }
}
