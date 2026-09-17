package com.dreamuniverse_server.user.entity;

import com.dreamuniverse_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(
        name = "uk_users_provider_user_id", columnNames = {"provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 255)
    private String name;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    // Nullable for legacy users; only completed signups may receive service tokens.
    @Column(length = 30)
    private String nickname;
    private LocalDate birthDate;
    private Instant termsOfServiceAgreedAt;
    private Instant privacyPolicyAgreedAt;
    private Instant signupCompletedAt;

    public static User create(OAuthProvider provider, String providerUserId) {
        var user = new User();
        user.provider = provider;
        user.providerUserId = providerUserId;
        return user;
    }

    public void updateTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isSignupCompleted() { return signupCompletedAt != null; }

    public void completeSignup(
            String email,
            String name,
            String nickname,
            LocalDate birthDate,
            String timezone,
            Instant now
    ) {
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.timezone = timezone;

        this.termsOfServiceAgreedAt = now;
        this.privacyPolicyAgreedAt = now;
        this.signupCompletedAt = now;
    }
}
