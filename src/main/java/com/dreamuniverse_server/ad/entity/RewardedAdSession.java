package com.dreamuniverse_server.ad.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(
        name = "rewarded_ad_sessions",
        indexes = {
                @Index(
                        name = "idx_rewarded_ad_sessions_user",
                        columnList = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardedAdSession {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardedAdStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "verification_expires_at", nullable = false)
    private Instant verificationExpiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "reward_expires_at")
    private Instant rewardExpiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(
            name = "admob_transaction_id",
            length = 128,
            unique = true
    )
    private String admobTransactionId;

    public static RewardedAdSession create(
            Long userId,
            Instant now
    ) {
        RewardedAdSession session = new RewardedAdSession();

        session.id = UUID.randomUUID().toString();
        session.userId = userId;
        session.status = RewardedAdStatus.PENDING;
        session.createdAt = now;
        session.verificationExpiresAt =
                now.plus(30, ChronoUnit.MINUTES);

        return session;
    }

    public void verify(String transactionId, Instant now) {
        if (status != RewardedAdStatus.PENDING
                || !now.isBefore(verificationExpiresAt)) {
            throw new IllegalStateException(
                    "Rewarded ad session cannot be verified"
            );
        }

        this.admobTransactionId = transactionId;
        this.verifiedAt = now;
        this.rewardExpiresAt =
                now.plus(10, ChronoUnit.MINUTES);
        this.status = RewardedAdStatus.VERIFIED;
    }

    public void consume(Instant now) {
        if (status != RewardedAdStatus.VERIFIED
                || rewardExpiresAt == null
                || !now.isBefore(rewardExpiresAt)) {
            throw new IllegalStateException(
                    "Rewarded ad reward is not available"
            );
        }

        this.status = RewardedAdStatus.CONSUMED;
        this.consumedAt = now;
    }
}
