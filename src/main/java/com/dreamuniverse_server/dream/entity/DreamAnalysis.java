package com.dreamuniverse_server.dream.entity;

import com.dreamuniverse_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "dream_analyses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dream_analyses_dream",
                        columnNames = "dream_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DreamAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dream_id", nullable = false)
    private Dream dream;

    @Column(nullable = false, length = 2000)
    private String interpretation;

    @Column(name = "symbolism_score", nullable = false)
    private int symbolismScore;

    @Column(name = "dynamism_score", nullable = false)
    private int dynamismScore;

    @Column(name = "fantasy_score", nullable = false)
    private int fantasyScore;

    public static DreamAnalysis create(
            Dream dream,
            String interpretation,
            int symbolismScore,
            int dynamismScore,
            int fantasyScore
    ) {
        validateScore(symbolismScore);
        validateScore(dynamismScore);
        validateScore(fantasyScore);

        DreamAnalysis analysis = new DreamAnalysis();

        analysis.dream = dream;
        analysis.interpretation = interpretation;
        analysis.symbolismScore = symbolismScore;
        analysis.dynamismScore = dynamismScore;
        analysis.fantasyScore = fantasyScore;

        return analysis;
    }

    public void update(
            String interpretation,
            int symbolismScore,
            int dynamismScore,
            int fantasyScore
    ) {
        validateScore(symbolismScore);
        validateScore(dynamismScore);
        validateScore(fantasyScore);

        this.interpretation = interpretation;
        this.symbolismScore = symbolismScore;
        this.dynamismScore = dynamismScore;
        this.fantasyScore = fantasyScore;
    }

    private static void validateScore(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                    "Dream analysis score must be between 0 and 100"
            );
        }
    }
}