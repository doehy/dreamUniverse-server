package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamElement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DreamElementRepository
        extends     JpaRepository<DreamElement, Long> {

    void deleteAllByAnalysisId(Long analysisId);
}