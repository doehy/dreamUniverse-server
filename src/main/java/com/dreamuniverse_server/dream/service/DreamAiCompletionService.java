package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DreamAiCompletionService {

    private final DreamAnalysisService dreamAnalysisService;
    private final DreamAiUsageService dreamAiUsageService;

    @Transactional
    public void complete(
            User user,
            Dream dream,
            DreamAnalysisResult result
    ) {
        // 분석 결과 저장
        dreamAnalysisService.saveAnalysis(
                dream,
                result
        );

        // AI 사용 횟수 증가
        dreamAiUsageService.use(user);
    }
}