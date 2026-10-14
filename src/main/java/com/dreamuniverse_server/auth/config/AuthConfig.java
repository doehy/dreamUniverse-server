package com.dreamuniverse_server.auth.config;

import java.util.Base64;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;


@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {
    public static final String ISSUER = "dreamuniverse-server";
    public static final String AUDIENCE = "dreamuniverse-api";

    @Bean
    SecretKey tokenKey(AuthProperties properties) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.token().secret());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JWT_SECRET must be Base64 encoded", e);
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 random bytes (Base64 encoded)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean("googleJwtDecoder")
    JwtDecoder googleJwtDecoder(AuthProperties properties) {
        var decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
        decoder.setJwtValidator(TokenValidators.forAudienceAndIssuers(properties.google().clientId(),
                Set.of("accounts.google.com", "https://accounts.google.com")));
        return decoder;
    }

    @Bean("accessTokenDecoder")
    JwtDecoder accessTokenDecoder(SecretKey tokenKey) {
        var decoder = NimbusJwtDecoder.withSecretKey(tokenKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                TokenValidators.forAudienceAndIssuers(AUDIENCE, Set.of(ISSUER)),
                new JwtClaimValidator<String>("token_use", "access"::equals),
                new JwtClaimValidator<String>("sub", TokenValidators::isUserId)));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("accessTokenDecoder") JwtDecoder decoder) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/google", "/api/auth/kakao", "/api/auth/signup", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        // Google AdMob SSV 콜백만 JWT 없이 허용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/ads/admob/ssv"
                        ).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder)))
                .build();
    }
}
