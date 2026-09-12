package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.Dream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DreamRepository extends JpaRepository<Dream, Long> {

    boolean existsByUserIdAndDreamDate(
            Long userId,
            LocalDate dreamDate
    );

    Optional<Dream> findByIdAndUserId(
            Long dreamId,
            Long userId
    );

    List<Dream> findAllByUserIdOrderByDreamDateDesc(
            Long userId
    );
}