package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamEmotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DreamEmotionRepository
        extends JpaRepository<DreamEmotion, Long> {

    void deleteAllByAnalysisId(Long analysisId);
}