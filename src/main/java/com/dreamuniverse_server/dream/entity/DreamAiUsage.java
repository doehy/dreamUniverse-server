package com.dreamuniverse_server.dream.entity;

import com.dreamuniverse_server.global.entity.BaseCreatedEntity;
import com.dreamuniverse_server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "dream_ai_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dream_ai_usages_user_date",
                        columnNames = {"user_id", "usage_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DreamAiUsage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    public static DreamAiUsage create(User user, LocalDate usageDate) {
        DreamAiUsage usage = new DreamAiUsage();
        usage.user = user;
        usage.usageDate = usageDate;
        usage.usedCount = 0;
        return usage;
    }

    public void increase() {
        this.usedCount++;
    }
}