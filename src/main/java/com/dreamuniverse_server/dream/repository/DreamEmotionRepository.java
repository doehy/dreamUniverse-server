package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamEmotion;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamEmotionScoreProjection;
import com.dreamuniverse_server.dream.statistics.repository.projection.DreamMonthlyEmotionScoreProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DreamEmotionRepository extends JpaRepository<DreamEmotion, Long> {

    List<DreamEmotion> findAllByAnalysisId(Long analysisId);

    @Modifying
    @Query("""
        DELETE FROM DreamEmotion e
        WHERE e.analysis.id = :analysisId
        """)
    int deleteAllByAnalysisId(
            @Param("analysisId") Long analysisId
    );

    @Query(
            value = """
                select
                    de.emotion_type as emotionType,
                    sum(de.score) as totalScore

                from dream_emotions de

                join dream_analyses da
                    on da.id = de.analysis_id

                join dreams d
                    on d.id = da.dream_id

                where d.user_id = :userId
                  and d.dream_date between :startDate and :endDate

                group by de.emotion_type

                order by sum(de.score) desc
                """,
            nativeQuery = true
    )
    List<DreamEmotionScoreProjection> findEmotionScores(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(
            value = """
                select
                    month(d.dream_date) as month,
                    de.emotion_type as emotionType,
                    sum(de.score) as totalScore

                from dream_emotions de

                join dream_analyses da
                    on da.id = de.analysis_id

                join dreams d
                    on d.id = da.dream_id

                where d.user_id = :userId
                  and d.dream_date between :startDate and :endDate

                group by
                    month(d.dream_date),
                    de.emotion_type

                order by
                    month(d.dream_date),
                    sum(de.score) desc
                """,
            nativeQuery = true
    )
    List<DreamMonthlyEmotionScoreProjection> findMonthlyEmotionScores(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}