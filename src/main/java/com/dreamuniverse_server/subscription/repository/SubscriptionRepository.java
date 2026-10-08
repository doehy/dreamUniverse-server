package com.dreamuniverse_server.subscription.repository;

import com.dreamuniverse_server.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);
}