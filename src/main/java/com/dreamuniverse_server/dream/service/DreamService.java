
package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.request.DreamCreateRequest;
import com.dreamuniverse_server.dream.dto.request.DreamUpdateRequest;
import com.dreamuniverse_server.dream.dto.response.DreamAiUsageResponse;
import com.dreamuniverse_server.dream.dto.response.DreamAnalysisResponse;
import com.dreamuniverse_server.dream.dto.response.DreamResponse;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAnalysis;
import com.dreamuniverse_server.dream.entity.DreamElement;
import com.dreamuniverse_server.dream.entity.DreamEmotion;
import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DreamService {

    private static final int DAILY_DREAM_LIMIT = 3;

    private final DreamRepository dreamRepository;
    private final DreamAnalysisRepository dreamAnalysisRepository;
    private final DreamEmotionRepository dreamEmotionRepository;
    private final DreamElementRepository dreamElementRepository;
    private final UserRepository userRepository;

    private final DreamCommandService dreamCommandService;
    private final DreamAiRequestService dreamAiRequestService;
    private final DreamAiStatusService dreamAiStatusService;
    private final DreamAiAsyncService dreamAiAsyncService;
    private final DreamAiUsageService dreamAiUsageService;
    private final DreamImageService dreamImageService;

    public DreamResponse createDream(
            Long userId,
            DreamCreateRequest request
    ) {
        User user = getUser(userId);

        long dreamCount = dreamRepository.countByUserIdAndDreamDate(
                user.getId(),
                request.dreamDate()
        );

        if (dreamCount >= DAILY_DREAM_LIMIT) {
            throw new CustomException(
                    ErrorCode.DREAM_DAILY_LIMIT_EXCEEDED
            );
        }

        Dream dream = dreamCommandService.create(
                user,
                request.title(),
                request.content(),
                request.dreamDate(),
                request.sleepParalysis(),
                request.nightmare(),
                request.lucidDream()
        );

        if (request.aiRequested()) {
            requestAiCompletion(
                    user,
                    dream
            );
        }

        return DreamResponse.from(dream);
    }

    @Transactional(readOnly = true)
    public DreamResponse getDream(
            Long userId,
            Long dreamId
    ) {
        Dream dream = getDreamByUser(dreamId, userId);
        return DreamResponse.from(dream);
    }

    @Transactional(readOnly = true)
    public List<DreamResponse> getDreams(Long userId) {
        User user = getUser(userId);

        return dreamRepository
                .findAllByUserIdOrderByDreamDateDesc(user.getId())
                .stream()
                .map(DreamResponse::from)
                .toList();
    }

    public DreamResponse updateDream(
            Long userId,
            Long dreamId,
            DreamUpdateRequest request
    ) {
        User user = getUser(userId);

        Dream dream = dreamCommandService.update(
                dreamId,
                userId,
                request.title(),
                request.content(),
                request.sleepParalysis(),
                request.nightmare(),
                request.lucidDream()
        );

        if (request.aiRequested()) {
            requestAiCompletion(
                    user,
                    dream
            );
        }

        return DreamResponse.from(dream);
    }

    @Transactional(readOnly = true)
    public DreamAiUsageResponse getAiUsage(Long userId) {
        User user = getUser(userId);

        int remainingCount =
                dreamAiUsageService.getRemainingCount(user);

        int dailyLimit =
                dreamAiUsageService.getDailyLimit(user);

        return new DreamAiUsageResponse(
                remainingCount,
                dailyLimit
        );
    }

    public void deleteDream(Long userId, Long dreamId) {
        User user = getUser(userId);

        List<String> imageKeys = dreamCommandService.delete(
                dreamId,
                user.getId()
        );

        dreamImageService.deleteOldImages(imageKeys);
    }

    @Transactional(readOnly = true)
    public DreamAnalysisResponse getAnalysis(
            Long userId,
            Long dreamId
    ) {
        User user = getUser(userId);

        Dream dream = dreamRepository
                .findByIdAndUserId(dreamId, user.getId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.DREAM_NOT_FOUND)
                );

        DreamAnalysis analysis = dreamAnalysisRepository
                .findByDreamId(dream.getId())
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DREAM_ANALYSIS_NOT_FOUND
                        )
                );

        List<DreamEmotion> emotions = dreamEmotionRepository
                .findAllByAnalysisId(analysis.getId());

        List<DreamElement> elements = dreamElementRepository
                .findAllByAnalysisId(analysis.getId());

        return DreamAnalysisResponse.from(
                analysis,
                emotions,
                elements
        );
    }

    public DreamResponse requestDreamAnalysis(
            Long userId,
            Long dreamId
    ) {
        User user = getUser(userId);

        // 기존 꿈 조회 및 소유권 확인
        Dream dream = getDreamByUser(dreamId, userId);

        // 기존 AI 요청 처리 로직 재사용
        requestAiCompletion(user, dream);

        return DreamResponse.from(dream);
    }


    private void requestAiCompletion(
            User user,
            Dream dream
    ) {
        LocalDate usageDate = dreamAiRequestService.prepare(
                user.getId(),
                user,
                dream.getId()
        );

        try {
            // prepare() 커밋 이후 DB에 저장된 최신 원문 조회
            Dream currentDream = getDreamByUser(
                    dream.getId(),
                    user.getId()
            );

            String content = currentDream.getContent();

            // 응답에 사용할 객체의 상태도 맞춘다.
            dream.startAiCompletion();

            // 비동기 AI 분석 시작
            dreamAiAsyncService.complete(
                    user.getId(),
                    dream.getId(),
                    content,
                    usageDate
            );

        } catch (RuntimeException e) {

            // 작업 제출 전 실패 시 이전 상태 복구
            try {
                dreamAiStatusService.restoreAfterFailure(
                        dream.getId()
                );
            } catch (RuntimeException statusException) {
                log.error(
                        "Failed to restore dream AI status. dreamId={}",
                        dream.getId(),
                        statusException
                );
            }

            // 사용량 반환
            try {
                dreamAiUsageService.release(
                        user.getId(),
                        usageDate
                );
            } catch (RuntimeException releaseException) {
                log.error(
                        "AI usage release failed. userId={}, usageDate={}",
                        user.getId(),
                        usageDate,
                        releaseException
                );
            }

            throw e;
        }
    }


    private User getUser(Long userId) {
        return userRepository
                .findById(userId)
                .filter(User::isSignupCompleted)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private Dream getDreamByUser(
            Long dreamId,
            Long userId
    ) {
        return dreamRepository
                .findByIdAndUserId(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.DREAM_NOT_FOUND)
                );
    }
}
