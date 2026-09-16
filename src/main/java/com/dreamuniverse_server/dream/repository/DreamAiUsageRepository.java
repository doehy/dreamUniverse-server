package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DreamAiUsageRepository
        extends JpaRepository<DreamAiUsage, Long> {

    Optional<DreamAiUsage> findByUserIdAndUsageDate(
            Long userId,
            LocalDate usageDate
    );

    /**
     * 오늘 사용량 row가 없으면 생성한다.
     *
     * 이미 존재하면 아무것도 변경하지 않는다.
     *
     * UNIQUE(user_id, usage_date)가 있기 때문에
     * 동시 요청이 들어와도 하루에 하나의 row만 존재한다.
     */
    @Modifying
    @Query(
            value = """
                    INSERT INTO dream_ai_usages (
                        user_id,
                        usage_date,
                        used_count,
                        created_at
                    )
                    VALUES (
                        :userId,
                        :usageDate,
                        0,
                        CURRENT_TIMESTAMP(6)
                    )
                    ON DUPLICATE KEY UPDATE
                        id = id
                    """,
            nativeQuery = true
    )
    void insertIfAbsent(
            @Param("userId") Long userId,
            @Param("usageDate") LocalDate usageDate
    );

    /**
     * 아직 일일 한도에 도달하지 않았을 때만 +1.
     *
     * 성공하면 affected rows = 1
     * 이미 한도에 도달했으면 affected rows = 0
     */
    @Modifying
    @Query(
            value = """
                    UPDATE dream_ai_usages
                    SET used_count = used_count + 1
                    WHERE user_id = :userId
                      AND usage_date = :usageDate
                      AND used_count < :dailyLimit
                    """,
            nativeQuery = true
    )
    int incrementIfAvailable(
            @Param("userId") Long userId,
            @Param("usageDate") LocalDate usageDate,
            @Param("dailyLimit") int dailyLimit
    );

    /**
     * AI 처리 실패 시 선점했던 사용 횟수를 반환한다.
     */
    @Modifying
    @Query(
            value = """
                    UPDATE dream_ai_usages
                    SET used_count = used_count - 1
                    WHERE user_id = :userId
                      AND usage_date = :usageDate
                      AND used_count > 0
                    """,
            nativeQuery = true
    )
    int decrement(
            @Param("userId") Long userId,
            @Param("usageDate") LocalDate usageDate
    );
}