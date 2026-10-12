package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DreamAnalysisRepository
        extends JpaRepository<DreamAnalysis, Long> {

    boolean existsByDreamId(Long dreamId);

    void deleteByDreamId(Long dreamId);

    Optional<DreamAnalysis> findByDreamId(Long dreamId);
}