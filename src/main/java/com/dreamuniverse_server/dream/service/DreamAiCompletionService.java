package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.entity.Dream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DreamAiCompletionService {

    private final DreamAnalysisService dreamAnalysisService;

    @Transactional
    public void complete(
            Dream dream,
            DreamAnalysisResult result
    ) {
        dreamAnalysisService.saveAnalysis(
                dream,
                result
        );
    }
}