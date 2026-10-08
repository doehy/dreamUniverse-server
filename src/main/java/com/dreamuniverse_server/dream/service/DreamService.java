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

    private final DreamAiUsageService dreamAiUsageService;
    private final DreamAiStatusService dreamAiStatusService;
    private final DreamAiAsyncService dreamAiAsyncService;


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

        // Transaction A
        // 사용자가 작성한 꿈 원문부터 먼저 저장한다.
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
                    dream,
                    request.content()
            );
        }

        return DreamResponse.from(dream);
    }


    @Transactional(readOnly = true)
    public DreamResponse getDream(
            Long userId,
            Long dreamId
    ) {
        Dream dream = getDreamByUser(
                dreamId,
                userId
        );

        return DreamResponse.from(dream);
    }


    @Transactional(readOnly = true)
    public List<DreamResponse> getDreams(
            Long userId
    ) {
        User user = getUser(userId);

        return dreamRepository
                .findAllByUserIdOrderByDreamDateDesc(
                        user.getId()
                )
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

        // Transaction A
        // 사용자가 수정한 원문부터 먼저 저장한다.
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
                    dream,
                    request.content()
            );
        }

        return DreamResponse.from(dream);
    }


    @Transactional(readOnly = true)
    public DreamAiUsageResponse getAiUsage(
            Long userId
    ) {
        User user = getUser(userId);

        int remainingCount =
                dreamAiUsageService
                        .getRemainingCount(user);

        int dailyLimit =
                dreamAiUsageService
                        .getDailyLimit(user);

        return new DreamAiUsageResponse(
                remainingCount,
                dailyLimit
        );
    }


    public void deleteDream(
            Long userId,
            Long dreamId
    ) {
        User user = getUser(userId);

        dreamCommandService.delete(
                dreamId,
                user.getId()
        );
    }


    @Transactional(readOnly = true)
    public DreamAnalysisResponse getAnalysis(
            Long userId,
            Long dreamId
    ) {
        User user = getUser(userId);

        Dream dream =
                dreamRepository
                        .findByIdAndUserId(
                                dreamId,
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new CustomException(
                                        ErrorCode.DREAM_NOT_FOUND
                                )
                        );

        DreamAnalysis analysis =
                dreamAnalysisRepository
                        .findByDreamId(
                                dream.getId()
                        )
                        .orElseThrow(
                                () -> new CustomException(
                                        ErrorCode.DREAM_ANALYSIS_NOT_FOUND
                                )
                        );

        List<DreamEmotion> emotions =
                dreamEmotionRepository
                        .findAllByAnalysisId(
                                analysis.getId()
                        );

        List<DreamElement> elements =
                dreamElementRepository
                        .findAllByAnalysisId(
                                analysis.getId()
                        );

        return DreamAnalysisResponse.from(
                analysis,
                emotions,
                elements
        );
    }


    private void requestAiCompletion(
            User user,
            Dream dream,
            String content
    ) {
        LocalDate usageDate =
                dreamAiUsageService.acquire(
                        user
                );

        try {
            /*
             * 별도 짧은 Transaction으로
             * DB 상태를 PROCESSING으로 변경한다.
             */
            dreamAiStatusService.markProcessing(
                    dream.getId()
            );

            /*
             * 현재 DreamService가 가지고 있는 dream 객체는
             * 위 Transaction에서 조회한 객체와 다른 인스턴스일 수 있다.
             *
             * 따라서 즉시 반환되는 DreamResponse에서도
             * PROCESSING을 보여주도록 현재 객체도 맞춰준다.
             */
            dream.startAiCompletion();

            /*
             * 여기서부터는 별도 Thread에서 실행된다.
             *
             * HTTP 요청은 이 호출 이후 AI 작업 완료를 기다리지 않는다.
             */
            dreamAiAsyncService.complete(
                    user.getId(),
                    dream.getId(),
                    content,
                    usageDate
            );

        } catch (RuntimeException e) {

            /*
             * 비동기 작업을 시작하기도 전에 실패했다면
             * 상태와 사용량을 복구한다.
             */
            try {
                dreamAiStatusService.markFailed(
                        dream.getId()
                );

                // 즉시 반환용 현재 객체 상태도 맞춘다.
                dream.failAiCompletion();

            } catch (RuntimeException statusException) {
                log.error(
                        "Failed to mark dream AI status as FAILED. dreamId={}",
                        dream.getId(),
                        statusException
                );
            }

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


    private User getUser(
            Long userId
    ) {
        return userRepository
                .findById(userId)
                .filter(User::isSignupCompleted)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }


    private Dream getDreamByUser(
            Long dreamId,
            Long userId
    ) {
        return dreamRepository
                .findByIdAndUserId(
                        dreamId,
                        userId
                )
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.DREAM_NOT_FOUND
                        )
                );
    }
}