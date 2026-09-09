package com.dreamuniverse_server.auth.repository;

import com.dreamuniverse_server.auth.entity.SignupSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SignupSessionRepository extends JpaRepository<SignupSession, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO auth_signup_sessions (provider, provider_user_id, token_hash, email, name, expires_at)
            VALUES (:provider, :providerUserId, :hash, :email, :name, :expiresAt)
            ON DUPLICATE KEY UPDATE token_hash = :hash, email = :email, name = :name,
                                    expires_at = :expiresAt, consumed_at = NULL
            """, nativeQuery = true)
    void upsert(@Param("provider") String provider, @Param("providerUserId") String providerUserId,
                @Param("hash") String hash, @Param("email") String email, @Param("name") String name,
                @Param("expiresAt") Instant expiresAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SignupSession s where s.tokenHash = :hash")
    Optional<SignupSession> findLockedByTokenHash(@Param("hash") String hash);
}
