package com.dreamuniverse_server.auth.service;

import com.dreamuniverse_server.auth.config.AuthProperties;
import com.dreamuniverse_server.auth.dto.response.*;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {
    @Qualifier("kakaoRestClient")
    private final RestClient restClient;
    private final AuthProperties authProperties;

    public UserInfo verify(String accessToken) {
        Long appId = authProperties.kakao().appId();
        if (appId == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kakao login is not configured");
        }
        try {
            var token = restClient.get().uri("/v1/user/access_token_info")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(KakaoTokenInfo.class);
            if (token == null || token.id() == null || token.id() <= 0
                    || token.appId() == null || token.expiresIn() == null) {
                throw unavailable();
            }
            if (!appId.equals(token.appId()) || token.expiresIn() <= 0) {
                throw unauthorized();
            }
            var user = restClient.get().uri(uri -> uri.path("/v2/user/me")
                            .queryParam("property_keys", "[\"kakao_account.email\",\"kakao_account.profile.nickname\"]")
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(KakaoUserResponse.class);
            if (user == null || user.id() == null) throw unavailable();
            if (!token.id().equals(user.id())) throw unauthorized();
            var account = user.account();
            if (account == null || account.email() == null || account.email().isBlank()
                    || !Boolean.TRUE.equals(account.emailValid())
                    || !Boolean.TRUE.equals(account.emailVerified())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kakao verified email consent required");
            }
            String name = account.profile() == null ? null : account.profile().nickname();
            return new UserInfo(OAuthProvider.KAKAO, user.id().toString(), account.email(), name);
        } catch (RestClientResponseException e) {
            // Never propagate provider bodies or access tokens to the client/logs.
            if (e.getStatusCode().value() == 401) throw unauthorized();
            throw unavailable();
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Kakao access token");
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kakao authentication temporarily unavailable");
    }
}
