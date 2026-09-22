package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.dreamuniverse_server.global.exception.ErrorCode.DREAM_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class DreamAiCompletionService {

    private final DreamRepository dreamRepository;

    private final DreamAnalysisService dreamAnalysisService;

    private final DreamImagePersistenceService
            dreamImagePersistenceService;

    @Transactional
    public List<String> complete(
            Long dreamId,
            DreamAnalysisResult analysisResult,
            String imageKey
    ) {
        Dream dream =
                dreamRepository.findById(dreamId)
                        .orElseThrow(
                                () -> new CustomException(
                                        DREAM_NOT_FOUND
                                )
                        );

        dreamAnalysisService.saveAnalysis(
                dream,
                analysisResult
        );

        List<String> oldImageKeys =
                dreamImagePersistenceService.replace(
                        dream,
                        imageKey
                );

        dream.completeAiCompletion();

        return oldImageKeys;
    }
}