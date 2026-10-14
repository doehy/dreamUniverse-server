package com.dreamuniverse_server.ad.repository;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RewardedAdSessionRepository
        extends JpaRepository<RewardedAdSession, String> {

    Optional<RewardedAdSession> findByIdAndUserId(
            String id,
            Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM RewardedAdSession s
            WHERE s.id = :sessionId
              AND s.userId = :userId
            """)
    Optional<RewardedAdSession> findByIdAndUserIdForUpdate(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM RewardedAdSession s
        WHERE s.id = :sessionId
        """)
    Optional<RewardedAdSession> findByIdForUpdate(
            @Param("sessionId") String sessionId
    ); // 동일 세션의 동시 검증을 직렬화

    // 이미 사용된 AdMob 거래 ID인지 확인
    boolean existsByAdmobTransactionId(String transactionId);
}
