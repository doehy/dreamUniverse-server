package com.dreamuniverse_server.dream.entity;

import com.dreamuniverse_server.global.entity.BaseTimeEntity;
import com.dreamuniverse_server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "dreams",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dreams_user_date",
                        columnNames = {"user_id", "dream_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dream extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 3000)
    private String content;

    @Column(name = "dream_date", nullable = false)
    private LocalDate dreamDate;

    public static Dream create(
            User user,
            String title,
            String content,
            LocalDate dreamDate
    ) {
        Dream dream = new Dream();
        dream.user = user;
        dream.title = title;
        dream.content = content;
        dream.dreamDate = dreamDate;
        return dream;
    }

    public void update(
            String title,
            String content
    ) {
        this.title = title;
        this.content = content;
    }
}