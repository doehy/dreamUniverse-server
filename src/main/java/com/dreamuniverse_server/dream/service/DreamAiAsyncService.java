package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamAiClient;
import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.repository.DreamRepository;
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
    private final DreamRepository dreamRepository;

    private final DreamAiClient dreamAiClient;
    private final DreamImageService dreamImageService;

    private final DreamAiCompletionService
            dreamAiCompletionService;

    private final DreamAiStatusService
            dreamAiStatusService;

    private final DreamAiUsageService
            dreamAiUsageService;

    @Async("dreamAiExecutor")
    public void complete(
            Long userId,
            Long dreamId,
            String content,
            LocalDate usageDate
    ) {
        String newImageKey = null;

        try {
            User user =
                    userRepository.findById(userId)
                            .orElseThrow();

            Dream dream =
                    dreamRepository.findById(dreamId)
                            .orElseThrow();


            /*
             * DB Transaction 없이
             * OpenAI 텍스트 호출
             */
            DreamAnalysisResult analysisResult =
                    dreamAiClient.analyze(
                            content
                    );


            /*
             * OpenAI 이미지 생성
             * +
             * R2 업로드
             */
            newImageKey =
                    dreamImageService.generateAndUpload(
                            user,
                            dream
                    );


            /*
             * 여기서만 짧은 DB Transaction
             *
             * 분석
             * 감정
             * 요소
             * 이미지
             * COMPLETED
             */
            List<String> oldImageKeys =
                    dreamAiCompletionService.complete(
                            dreamId,
                            analysisResult,
                            newImageKey
                    );


            /*
             * DB가 새 이미지로 정상 전환된 뒤
             * 과거 R2 이미지를 정리한다.
             *
             * 삭제 실패는 전체 실패로 보지 않는다.
             */
            dreamImageService.deleteOldImages(
                    oldImageKeys
            );

        } catch (RuntimeException e) {

            log.error(
                    "Dream AI completion failed. userId={}, dreamId={}",
                    userId,
                    dreamId,
                    e
            );


            /*
             * 새 R2 업로드까지 성공한 상황에서
             * 이후 DB 저장이 실패했다면
             * orphan 파일 정리
             */
            if (newImageKey != null) {
                dreamImageService.deleteUploadedImage(
                        newImageKey
                );
            }


            try {
                dreamAiStatusService.markFailed(
                        dreamId
                );

            } catch (RuntimeException statusException) {
                log.error(
                        "Failed to mark dream AI status as FAILED. dreamId={}",
                        dreamId,
                        statusException
                );
            }


            try {
                dreamAiUsageService.release(
                        userId,
                        usageDate
                );

            } catch (RuntimeException releaseException) {
                log.error(
                        "AI usage release failed. userId={}, usageDate={}",
                        userId,
                        usageDate,
                        releaseException
                );
            }
        }
    }
}