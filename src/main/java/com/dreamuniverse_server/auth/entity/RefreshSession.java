package com.dreamuniverse_server.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "auth_refresh_sessions", indexes = @Index(name = "idx_refresh_session_expiry", columnList = "expires_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    private Instant revokedAt;

    public static RefreshSession create(Long userId, Instant expiresAt) {
        var session = new RefreshSession();
        session.userId = userId;
        session.expiresAt = expiresAt;
        return session;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) revokedAt = now;
    }
}
