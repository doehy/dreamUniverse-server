
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

    /**
     * 꿈 작성
     *
     * 꿈 원문만 저장한다.
     * AI 분석은 별도의 POST /dreams/{dreamId}/analysis로 요청한다.
     */
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

        return DreamResponse.from(dream);
    }

    /**
     * 꿈 단건 조회
     */
    @Transactional(readOnly = true)
    public DreamResponse getDream(
            Long userId,
            Long dreamId
    ) {
        Dream dream = getDreamByUser(dreamId, userId);

        return DreamResponse.from(dream);
    }

    /**
     * 내가 작성한 꿈 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DreamResponse> getDreams(Long userId) {
        User user = getUser(userId);

        return dreamRepository
                .findAllByUserIdOrderByDreamDateDesc(user.getId())
                .stream()
                .map(DreamResponse::from)
                .toList();
    }

    /**
     * 꿈 수정
     *
     * 원문 및 꿈 속성만 수정한다.
     * AI 분석 요청은 처리하지 않는다.
     */
    public DreamResponse updateDream(
            Long userId,
            Long dreamId,
            DreamUpdateRequest request
    ) {
        User user = getUser(userId);

        Dream dream = dreamCommandService.update(
                dreamId,
                user.getId(),
                request.title(),
                request.content(),
                request.sleepParalysis(),
                request.nightmare(),
                request.lucidDream()
        );

        return DreamResponse.from(dream);
    }

    /**
     * AI 사용량 조회
     */
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

    /**
     * 꿈과 관련 이미지를 삭제한다.
     *
     * R2 이미지 삭제에 성공한 경우에만 DB 삭제를 진행한다.
     * R2 삭제 실패 시 예외를 전파하여 DB 삭제를 중단한다.
     */
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

    /**
     * 저장된 AI 분석 결과 조회
     */
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

    /**
     * AI 분석 / 재분석 요청
     *
     * POST /dreams/{dreamId}/analysis
     *
     * 저장된 꿈 원문을 기준으로 AI 분석을 시작한다.
     */
    public DreamResponse requestDreamAnalysis(
            Long userId,
            Long dreamId,
            String adSessionId
    ) {
        User user = getUser(userId);

        // 꿈 존재 및 소유권 확인
        Dream dream = getDreamByUser(dreamId, userId);

        // 사용량 차감, PROCESSING 전환, 비동기 작업 시작
        requestAiCompletion(user, dream, adSessionId);

        // PROCESSING 상태 반환
        return DreamResponse.from(dream);
    }

    /**
     * 실제 AI 요청 처리
     *
     * 사용량 차감과 PROCESSING 변경이 커밋된 후
     * 별도 스레드에서 AI 작업을 실행한다.
     */
    private void requestAiCompletion(
            User user,
            Dream dream,
            String adSessionId
    ) {
        LocalDate usageDate = dreamAiRequestService.prepare(
                user.getId(),
                user,
                dream.getId(),
                adSessionId
        );

        try {
            // PROCESSING 전환이 커밋된 이후 원문 조회
            Dream currentDream = getDreamByUser(
                    dream.getId(),
                    user.getId()
            );

            String content = currentDream.getContent();

            // 현재 응답용 엔티티도 PROCESSING으로 표시
            dream.startAiCompletion();

            // 비동기 AI 작업 시작
            dreamAiAsyncService.complete(
                    user.getId(),
                    dream.getId(),
                    content,
                    usageDate
            );

        } catch (RuntimeException e) {

            log.error(
                    "Failed to submit dream AI request. dreamId={}",
                    dream.getId(),
                    e
            );

            // 작업 제출 전 실패 시 상태 복구
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

            // 차감된 AI 사용량 반환
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

    /**
     * 가입이 완료된 사용자 조회
     */
    private User getUser(Long userId) {
        return userRepository
                .findById(userId)
                .filter(User::isSignupCompleted)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );
    }

    /**
     * 사용자 소유 꿈 조회
     */
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
