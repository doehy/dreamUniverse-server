
package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.Dream;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DreamDeletionRepository extends Repository<Dream, Long> {

    // 1. 이미지 레코드 삭제
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            DELETE FROM DreamImage image
            WHERE image.dream.id = :dreamId
            """)
    int deleteImages(@Param("dreamId") Long dreamId);

    // 2. 분석에 연결된 감정 삭제
    @Modifying
    @Query("""
            DELETE FROM DreamEmotion emotion
            WHERE emotion.analysis.id IN (
                SELECT analysis.id
                FROM DreamAnalysis analysis
                WHERE analysis.dream.id = :dreamId
            )
            """)
    int deleteEmotions(@Param("dreamId") Long dreamId);

    // 3. 분석에 연결된 요소 삭제
    @Modifying
    @Query("""
            DELETE FROM DreamElement element
            WHERE element.analysis.id IN (
                SELECT analysis.id
                FROM DreamAnalysis analysis
                WHERE analysis.dream.id = :dreamId
            )
            """)
    int deleteElements(@Param("dreamId") Long dreamId);

    // 4. 분석 데이터 삭제
    @Modifying
    @Query("""
            DELETE FROM DreamAnalysis analysis
            WHERE analysis.dream.id = :dreamId
            """)
    int deleteAnalysis(@Param("dreamId") Long dreamId);

    // 5. 꿈 원본 삭제
    @Modifying
    @Query("""
            DELETE FROM Dream dream
            WHERE dream.id = :dreamId
              AND dream.user.id = :userId
            """)
    int deleteDream(
            @Param("dreamId") Long dreamId,
            @Param("userId") Long userId
    );
}
