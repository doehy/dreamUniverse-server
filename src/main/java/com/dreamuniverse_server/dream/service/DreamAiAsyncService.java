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

        try {
            User user =
                    userRepository.findById(userId)
                            .orElseThrow();


            /*
             * OpenAI 텍스트 분석
             *
             * AI 요청 당시 전달받은 content를 사용한다.
             */
            DreamAnalysisResult analysisResult =
                    dreamAiClient.analyze(
                            content
                    );


            /*
             * OpenAI 이미지 생성
             * +
             * R2 업로드
             *
             * 이미지 역시 AI 요청 당시의 content를 사용한다.
             *
             * 따라서 비동기 처리 중 사용자가 꿈 내용을 수정해도
             * 텍스트 분석과 이미지가 서로 다른 내용을 바라보지 않는다.
             */
            newImageKey =
                    dreamImageService.generateAndUpload(
                            user,
                            dreamId,
                            content
                    );


            /*
             * 짧은 DB Transaction
             *
             * - 해몽 저장
             * - 감정 저장
             * - 요소 저장
             * - DreamImage 저장
             * - aiStatus = COMPLETED
             *
             * 실제 이미지 DB 저장은
             * DreamImagePersistenceService가 담당한다.
             */
            List<String> oldImageKeys =
                    dreamAiCompletionService.complete(
                            dreamId,
                            analysisResult,
                            newImageKey
                    );


            /*
             * DB가 새로운 이미지로 정상 전환된 뒤
             * 이전 R2 이미지를 정리한다.
             *
             * 기존 이미지 삭제 실패는
             * AI 완료 자체의 실패로 처리하지 않는다.
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
             * 이미지 생성 + R2 업로드까지 성공했지만
             * 이후 DB Transaction에서 실패했다면
             * 새로 업로드한 R2 파일이 orphan이 되므로 삭제한다.
             */
            if (newImageKey != null) {
                dreamImageService.deleteUploadedImage(
                        newImageKey
                );
            }


            /*
             * AI 처리 상태를 FAILED로 변경한다.
             */
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


            /*
             * AI 작업이 실패했으므로
             * 차감했던 일일 AI 사용 횟수를 복구한다.
             */
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