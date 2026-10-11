
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
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
                .findByIdAndUserId(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DREAM_NOT_FOUND
                        )
                );

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

        // 1. 꿈 존재 및 소유권 확인
        dreamRepository
                .findByIdAndUserId(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DREAM_NOT_FOUND
                        )
                );

        // 2. R2에서 삭제할 이미지 키 확보
        List<String> imageKeys = dreamImageRepository
                .findAllByDreamIdOrderByImageOrderAsc(dreamId)
                .stream()
                .map(DreamImage::getImageKey)
                .toList();

        // 3. 이미지 레코드 삭제
        // clearAutomatically로 영속성 컨텍스트 정리
        dreamDeletionRepository.deleteImages(dreamId);

        // 4. 분석에 연결된 감정 삭제
        dreamDeletionRepository.deleteEmotions(dreamId);

        // 5. 분석에 연결된 요소 삭제
        dreamDeletionRepository.deleteElements(dreamId);

        // 6. 분석 데이터 삭제
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

        // 8. 트랜잭션 커밋 이후 R2 삭제에 사용할 키 반환
        return imageKeys;
    }
}
