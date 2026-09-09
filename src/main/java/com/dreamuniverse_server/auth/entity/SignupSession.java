package com.dreamuniverse_server.auth.entity;

import com.dreamuniverse_server.user.entity.OAuthProvider;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "auth_signup_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_signup_provider_user", columnNames = {"provider", "provider_user_id"}),
        @UniqueConstraint(name = "uk_signup_token_hash", columnNames = "token_hash")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignupSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private OAuthProvider provider;
    @Column(nullable = false, length = 255)
    private String providerUserId;
    @Column(nullable = false, length = 64)
    private String tokenHash;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(length = 255)
    private String name;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant consumedAt;

    public boolean isActive(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) { consumedAt = now; }
}
