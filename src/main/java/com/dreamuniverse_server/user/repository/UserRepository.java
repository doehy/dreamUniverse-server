package com.dreamuniverse_server.user.repository;

import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
