package com.dreamuniverse_server.dream.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "dream_emotions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dream_emotions_analysis_type",
                        columnNames = {"analysis_id", "emotion_type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DreamEmotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DreamAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "emotion_type",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30)"
    )
    private DreamEmotionType emotionType;

    @Column(nullable = false)
    private int score;

    public static DreamEmotion create(
            DreamAnalysis analysis,
            DreamEmotionType emotionType,
            int score
    ) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                    "Emotion score must be between 0 and 100"
            );
        }

        DreamEmotion emotion = new DreamEmotion();
        emotion.analysis = analysis;
        emotion.emotionType = emotionType;
        emotion.score = score;

        return emotion;
    }
}