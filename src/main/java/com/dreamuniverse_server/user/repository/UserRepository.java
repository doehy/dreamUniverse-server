package com.dreamuniverse_server.user.repository;

import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    // MySQL atomic upsert: simultaneous first logins must resolve to one internal user ID.
    @Modifying
    @Query(value = """
            INSERT INTO users (provider, provider_user_id, email, name)
            VALUES (:provider, :providerUserId, :email, :name)
            ON DUPLICATE KEY UPDATE email = :email, name = :name
            """, nativeQuery = true)
    void upsertOAuthUser(@Param("provider") String provider, @Param("providerUserId") String providerUserId,
                         @Param("email") String email, @Param("name") String name);
}
