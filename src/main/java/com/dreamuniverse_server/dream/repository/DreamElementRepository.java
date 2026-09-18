package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DreamElementRepository extends JpaRepository<DreamElement, Long> {

    List<DreamElement> findAllByAnalysisId(Long analysisId);

    void deleteAllByAnalysisId(Long analysisId);
}