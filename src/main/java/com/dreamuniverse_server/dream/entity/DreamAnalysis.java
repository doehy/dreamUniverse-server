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

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, length = 2000)
    private String interpretation;

    public static DreamAnalysis create(
            Dream dream,
            String summary,
            String interpretation
    ) {
        DreamAnalysis analysis = new DreamAnalysis();
        analysis.dream = dream;
        analysis.summary = summary;
        analysis.interpretation = interpretation;

        return analysis;
    }

    public void update(
            String summary,
            String interpretation
    ) {
        this.summary = summary;
        this.interpretation = interpretation;
    }
}