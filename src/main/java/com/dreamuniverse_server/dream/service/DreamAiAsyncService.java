package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamAiClient;
import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
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

    @Async("dreamAiExecutor")
    public void complete(
            Long userId,
            Long dreamId,
            String content,
            LocalDate usageDate
    ) {
        String newImageKey = null;

        log.info(
                "[Dream AI] START userId={}, dreamId={}",
                userId,
                dreamId
        );

        try {
            log.info(
                    "[Dream AI] User load START dreamId={}",
                    dreamId
            );

            User user =
                    userRepository.findById(userId)
                            .orElseThrow();

            log.info(
                    "[Dream AI] User load COMPLETED dreamId={}",
                    dreamId
            );


            /*
             * OpenAI 텍스트 분석
             */
            log.info(
                    "[Dream AI] Text analysis START dreamId={}",
                    dreamId
            );

            DreamAnalysisResult analysisResult =
                    dreamAiClient.analyze(
                            content
                    );

            log.info(
                    "[Dream AI] Text analysis COMPLETED dreamId={}",
                    dreamId
            );


            /*
             * OpenAI 이미지 생성 + R2 업로드
             */
            log.info(
                    "[Dream AI] Image generation/upload START dreamId={}",
                    dreamId
            );

            newImageKey =
                    dreamImageService.generateAndUpload(
                            user,
                            dreamId,
                            content
                    );

            log.info(
                    "[Dream AI] Image generation/upload COMPLETED dreamId={}, imageKey={}",
                    dreamId,
                    newImageKey
            );


            /*
             * 분석 / 이미지 DB 저장
             * + COMPLETED 상태 변경
             */
            log.info(
                    "[Dream AI] DB completion START dreamId={}",
                    dreamId
            );

            List<String> oldImageKeys =
                    dreamAiCompletionService.complete(
                            dreamId,
                            analysisResult,
                            newImageKey
                    );

            log.info(
                    "[Dream AI] DB completion COMPLETED dreamId={}",
                    dreamId
            );


            /*
             * 기존 R2 이미지 정리
             */
            log.info(
                    "[Dream AI] Old image cleanup START dreamId={}, count={}",
                    dreamId,
                    oldImageKeys.size()
            );

            dreamImageService.deleteOldImages(
                    oldImageKeys
            );

            log.info(
                    "[Dream AI] Old image cleanup COMPLETED dreamId={}",
                    dreamId
            );

            log.info(
                    "[Dream AI] COMPLETED userId={}, dreamId={}",
                    userId,
                    dreamId
            );

        } catch (RuntimeException e) {

            log.error(
                    "[Dream AI] FAILED userId={}, dreamId={}",
                    userId,
                    dreamId,
                    e
            );


            /*
             * R2에는 올라갔는데 DB 반영 전에 실패한 경우
             */
            if (newImageKey != null) {

                try {
                    dreamImageService.deleteUploadedImage(
                            newImageKey
                    );

                } catch (RuntimeException deleteException) {

                    log.error(
                            "[Dream AI] Uploaded image cleanup FAILED. dreamId={}, imageKey={}",
                            dreamId,
                            newImageKey,
                            deleteException
                    );
                }
            }


            /*
             * FAILED 상태 변경
             */
            try {

                log.info(
                        "[Dream AI] Mark FAILED START dreamId={}",
                        dreamId
                );

                dreamAiStatusService.markFailed(
                        dreamId
                );

                log.info(
                        "[Dream AI] Mark FAILED COMPLETED dreamId={}",
                        dreamId
                );

            } catch (RuntimeException statusException) {

                log.error(
                        "[Dream AI] Failed to mark AI status as FAILED. dreamId={}",
                        dreamId,
                        statusException
                );
            }


            /*
             * AI 사용 횟수 복구
             */
            try {

                dreamAiUsageService.release(
                        userId,
                        usageDate
                );

                log.info(
                        "[Dream AI] Usage released. userId={}, dreamId={}, usageDate={}",
                        userId,
                        dreamId,
                        usageDate
                );

            } catch (RuntimeException releaseException) {

                log.error(
                        "[Dream AI] AI usage release FAILED. userId={}, dreamId={}, usageDate={}",
                        userId,
                        dreamId,
                        usageDate,
                        releaseException
                );
            }
        }
    }
}