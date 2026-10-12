
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
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
    private final DreamImagePersistenceService dreamImagePersistenceService;

    @Transactional
    public List<String> complete(
            Long dreamId,
            DreamAnalysisResult analysisResult,
            String imageKey
    ) {
        // AI 시작 및 삭제와 동일한 행 잠금을 사용한다.
        Dream dream = dreamRepository
                .findByIdForUpdate(dreamId)
                .orElseThrow(() ->
                        new CustomException(DREAM_NOT_FOUND)
                );

        if (dream.getAiStatus() != DreamAiStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Dream is not processing. dreamId=" + dreamId
            );
        }

        // 1. 기존 분석을 업데이트하거나 신규 생성
        // 감정 및 요소도 새 결과로 교체
        dreamAnalysisService.saveAnalysis(
                dream,
                analysisResult
        );

        // 2. DB 이미지 레코드 교체
        List<String> oldImageKeys =
                dreamImagePersistenceService.replace(
                        dream,
                        imageKey
                );

        // 3. 모든 처리가 성공했을 때 COMPLETED 전환
        dream.completeAiCompletion();

        return oldImageKeys;
    }
}
