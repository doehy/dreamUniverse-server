package com.dreamuniverse_server.subscription.entity;

import com.dreamuniverse_server.global.entity.BaseTimeEntity;
import com.dreamuniverse_server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscriptions_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private SubscriptionStatus status;

    @Column(
            name = "started_at",
            nullable = false
    )
    private LocalDateTime startedAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;


    public static Subscription createPremium(
            User user,
            LocalDateTime startedAt,
            LocalDateTime expiresAt
    ) {
        Subscription subscription = new Subscription();

        subscription.user = user;
        subscription.plan = SubscriptionPlan.PREMIUM;
        subscription.status = SubscriptionStatus.ACTIVE;
        subscription.startedAt = startedAt;
        subscription.expiresAt = expiresAt;

        return subscription;
    }


    public void renew(
            LocalDateTime startedAt,
            LocalDateTime expiresAt
    ) {
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }


    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
    }


    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }


    public boolean isPremium(
            LocalDateTime now
    ) {
        return plan == SubscriptionPlan.PREMIUM
                && status != SubscriptionStatus.EXPIRED
                && expiresAt.isAfter(now);
    }
}