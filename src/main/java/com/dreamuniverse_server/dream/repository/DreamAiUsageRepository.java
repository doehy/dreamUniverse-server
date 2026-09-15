package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamAiUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DreamAiUsageRepository extends JpaRepository<DreamAiUsage, Long> {

    Optional<DreamAiUsage> findByUserIdAndUsageDate(
            Long userId,
            LocalDate usageDate
    );
}