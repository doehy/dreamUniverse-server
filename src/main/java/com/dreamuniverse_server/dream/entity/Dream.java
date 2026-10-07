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
        indexes = {
                @Index(
                        name = "idx_dreams_user_date",
                        columnList = "user_id, dream_date"
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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "ai_status",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20)"
    )
    private DreamAiStatus aiStatus;

    @Column(name = "dream_date", nullable = false)
    private LocalDate dreamDate;

    @Column(nullable = false, length = 3000)
    private String content;

    @Column(name = "sleep_paralysis", nullable = false)
    private boolean sleepParalysis;

    @Column(name = "nightmare", nullable = false)
    private boolean nightmare;

    @Column(name = "lucid_dream", nullable = false)
    private boolean lucidDream;

    public static Dream create(
            User user,
            String title,
            String content,
            LocalDate dreamDate,
            boolean sleepParalysis,
            boolean nightmare,
            boolean lucidDream
    ) {
        Dream dream = new Dream();

        dream.user = user;
        dream.title = title;
        dream.content = content;
        dream.dreamDate = dreamDate;

        dream.sleepParalysis = sleepParalysis;
        dream.nightmare = nightmare;
        dream.lucidDream = lucidDream;

        dream.aiStatus = DreamAiStatus.NOT_REQUESTED;

        return dream;
    }

    public void update(
            String title,
            String content,
            boolean sleepParalysis,
            boolean nightmare,
            boolean lucidDream
    ) {
        this.title = title;
        this.content = content;

        this.sleepParalysis = sleepParalysis;
        this.nightmare = nightmare;
        this.lucidDream = lucidDream;
    }

    public void startAiCompletion() {
        this.aiStatus = DreamAiStatus.PROCESSING;
    }

    public void completeAiCompletion() {
        this.aiStatus = DreamAiStatus.COMPLETED;
    }

    public void failAiCompletion() {
        this.aiStatus = DreamAiStatus.FAILED;
    }
}