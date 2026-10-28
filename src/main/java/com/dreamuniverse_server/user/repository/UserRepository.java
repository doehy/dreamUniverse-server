package com.dreamuniverse_server.user.repository;

import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
