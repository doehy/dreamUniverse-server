package com.dreamuniverse_server.auth.repository;

import com.dreamuniverse_server.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // Read only the ID before taking the session lock, avoiding stale managed token state.
    @Query("select t.sessionId from RefreshToken t where t.tokenHash = :hash")
    Optional<Long> findSessionId(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> findLockedByHash(@Param("hash") String hash);
}
