
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamAiClient;
import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.validation.DreamAnalysisValidator;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DreamAiAsyncService {

    private final UserRepository userRepository;

    private final DreamAiClient dreamAiClient;
    private final DreamImageService dreamImageService;

    private final DreamAiCompletionService dreamAiCompletionService;
    private final DreamAiStatusService dreamAiStatusService;
    private final DreamAiUsageService dreamAiUsageService;
    private final DreamAnalysisValidator dreamAnalysisValidator;

    @Async("dreamAiExecutor")
    public void complete(
            Long userId,
            Long dreamId,
            String content,
            LocalDate usageDate
    ) {
        String newImageKey = null;
        List<String> oldImageKeys;

        log.info(
                "[Dream AI] START userId={}, dreamId={}",
                userId,
                dreamId
        );

        try {
            // 1. 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow();

            // 2. 텍스트 분석
            log.info(
                    "[Dream AI] Text analysis START dreamId={}",
                    dreamId
            );

            DreamAnalysisResult analysisResult =
                    dreamAiClient.analyze(content);

            log.info(
                    "[Dream AI] Text analysis COMPLETED dreamId={}",
                    dreamId
            );

            // 2. 분석 결과 검증
            // 실패하면 이미지 생성 전에 예외 발생
            dreamAnalysisValidator.validate(analysisResult);

            // 3. 이미지 생성 및 R2 업로드
            log.info(
                    "[Dream AI] Image generation START dreamId={}",
                    dreamId
            );

            newImageKey = dreamImageService.generateAndUpload(
                    user,
                    dreamId,
                    content
            );

            log.info(
                    "[Dream AI] Image upload COMPLETED dreamId={}, key={}",
                    dreamId,
                    newImageKey
            );

            // 4. DB 트랜잭션에서 분석과 이미지 교체
            // 이 메서드가 정상 반환되면 DB 커밋 완료
            oldImageKeys = dreamAiCompletionService.complete(
                    dreamId,
                    analysisResult,
                    newImageKey
            );

        } catch (RuntimeException e) {

            log.error(
                    "[Dream AI] FAILED userId={}, dreamId={}",
                    userId,
                    dreamId,
                    e
            );

            // 5. 이번 요청에서 새로 올린 이미지 정리
            // 기존 이미지는 절대로 삭제하지 않는다.
            if (newImageKey != null) {
                try {
                    dreamImageService.deleteUploadedImage(
                            newImageKey
                    );
                } catch (RuntimeException deleteException) {
                    log.error(
                            "[Dream AI] New image cleanup FAILED. dreamId={}, key={}",
                            dreamId,
                            newImageKey,
                            deleteException
                    );
                }
            }

            // 6. 기존 결과 유무에 따라 상태 복구
            try {
                var restoredStatus =
                        dreamAiStatusService.restoreAfterFailure(
                                dreamId
                        );

                log.info(
                        "[Dream AI] Status restored. dreamId={}, status={}",
                        dreamId,
                        restoredStatus
                );

            } catch (RuntimeException statusException) {
                log.error(
                        "[Dream AI] Status restore FAILED. dreamId={}",
                        dreamId,
                        statusException
                );
            }

            // 7. AI 사용량 반환
            try {
                dreamAiUsageService.release(
                        userId,
                        usageDate
                );

                log.info(
                        "[Dream AI] Usage released. userId={}, dreamId={}",
                        userId,
                        dreamId
                );

            } catch (RuntimeException releaseException) {
                log.error(
                        "[Dream AI] Usage release FAILED. userId={}, dreamId={}",
                        userId,
                        dreamId,
                        releaseException
                );
            }

            return;
        }

        // 8. 여기부터는 DB 반영에 성공한 경우에만 실행된다.
        log.info(
                "[Dream AI] DB completion SUCCESS dreamId={}",
                dreamId
        );

        // 9. 이전 R2 이미지 정리
        // 실패하더라도 새 분석의 성공 상태는 유지한다.
        try {
            dreamImageService.deleteOldImages(oldImageKeys);

            log.info(
                    "[Dream AI] Old images cleanup COMPLETED dreamId={}, count={}",
                    dreamId,
                    oldImageKeys.size()
            );

        } catch (RuntimeException cleanupException) {
            log.error(
                    "[Dream AI] Old images cleanup FAILED. dreamId={}",
                    dreamId,
                    cleanupException
            );
        }

        log.info(
                "[Dream AI] COMPLETED userId={}, dreamId={}",
                userId,
                dreamId
        );
    }
}
