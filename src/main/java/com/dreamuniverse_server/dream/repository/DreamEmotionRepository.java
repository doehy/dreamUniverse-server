package com.dreamuniverse_server.dream.repository;

import com.dreamuniverse_server.dream.entity.DreamEmotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DreamEmotionRepository extends JpaRepository<DreamEmotion, Long> {

    List<DreamEmotion> findAllByAnalysisId(Long analysisId);

    void deleteAllByAnalysisId(Long analysisId);
}