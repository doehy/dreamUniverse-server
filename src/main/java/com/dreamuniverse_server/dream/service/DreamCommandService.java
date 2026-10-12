
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.entity.DreamImage;
import com.dreamuniverse_server.dream.repository.DreamDeletionRepository;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DreamCommandService {

    private final DreamRepository dreamRepository;
    private final DreamImageRepository dreamImageRepository;
    private final DreamDeletionRepository dreamDeletionRepository;

    @Transactional
    public Dream create(
            User user,
            String title,
            String content,
            LocalDate dreamDate,
            boolean sleepParalysis,
            boolean nightmare,
            boolean lucidDream
    ) {
        Dream dream = Dream.create(
                user,
                title,
                content,
                dreamDate,
                sleepParalysis,
                nightmare,
                lucidDream
        );

        return dreamRepository.save(dream);
    }

    @Transactional
    public Dream update(
            Long dreamId,
            Long userId,
            String title,
            String content,
            boolean sleepParalysis,
            boolean nightmare,
            boolean lucidDream
    ) {
        Dream dream = dreamRepository
                .findByIdAndUserIdForUpdate(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.DREAM_NOT_FOUND)
                );

        // AI 분석 중에는 원문 변경 차단
        if (dream.getAiStatus() == DreamAiStatus.PROCESSING) {
            throw new CustomException(
                    ErrorCode.DREAM_UPDATE_WHILE_PROCESSING
            );
        }

        dream.update(
                title,
                content,
                sleepParalysis,
                nightmare,
                lucidDream
        );

        return dream;
    }

    @Transactional
    public List<String> delete(Long dreamId, Long userId) {

        // 1. 소유권 확인 및 행 잠금
        Dream dream = dreamRepository
                .findByIdAndUserIdForUpdate(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.DREAM_NOT_FOUND)
                );

        // 2. 분석 중 삭제 차단
        if (dream.getAiStatus() == DreamAiStatus.PROCESSING) {
            throw new CustomException(
                    ErrorCode.DREAM_AI_PROCESSING
            );
        }

        // 3. R2 이미지 키 확보
        List<String> imageKeys = dreamImageRepository
                .findAllByDreamIdOrderByImageOrderAsc(dreamId)
                .stream()
                .map(DreamImage::getImageKey)
                .toList();

        // 4. 이미지 DB 레코드 삭제 및 영속성 컨텍스트 정리
        dreamDeletionRepository.deleteImages(dreamId);

        // 5. 분석 하위 데이터 삭제
        dreamDeletionRepository.deleteEmotions(dreamId);
        dreamDeletionRepository.deleteElements(dreamId);

        // 6. 분석 삭제
        dreamDeletionRepository.deleteAnalysis(dreamId);

        // 7. 꿈 원본 삭제
        int deletedCount = dreamDeletionRepository.deleteDream(
                dreamId,
                userId
        );

        if (deletedCount != 1) {
            throw new IllegalStateException(
                    "Dream deletion failed. dreamId=" + dreamId
            );
        }

        return imageKeys;
    }
}
