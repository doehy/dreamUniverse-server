
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DreamAiStatusService {

    private final DreamRepository dreamRepository;
    private final DreamAnalysisRepository dreamAnalysisRepository;

    @Transactional
    public DreamAiStatus restoreAfterFailure(Long dreamId) {

        Dream dream = dreamRepository
                .findByIdForUpdate(dreamId)
                .orElseThrow();

        // PROCESSING 상태가 아니면 다른 완료 상태를 덮어쓰지 않는다.
        if (dream.getAiStatus() != DreamAiStatus.PROCESSING) {
            return dream.getAiStatus();
        }

        // 기존에 성공적으로 저장된 분석이 있는지 확인
        boolean hasPreviousResult =
                dreamAnalysisRepository.existsByDreamId(dreamId);

        if (hasPreviousResult) {
            // 재분석 실패: 기존 결과를 유지하고 COMPLETED 복구
            dream.completeAiCompletion();
        } else {
            // 최초 분석 실패
            dream.failAiCompletion();
        }

        return dream.getAiStatus();
    }
}
