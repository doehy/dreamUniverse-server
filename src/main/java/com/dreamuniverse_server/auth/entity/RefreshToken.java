package com.dreamuniverse_server.auth.entity;

import com.dreamuniverse_server.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "auth_refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "token_hash"),
        indexes = @Index(name = "idx_refresh_token_session", columnList = "session_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    private Instant consumedAt;

    public static RefreshToken create(String hash, Long sessionId) {
        var token = new RefreshToken();
        token.tokenHash = hash;
        token.sessionId = sessionId;
        return token;
    }

    public void consume(Instant now) {
        consumedAt = now;
    }
}
