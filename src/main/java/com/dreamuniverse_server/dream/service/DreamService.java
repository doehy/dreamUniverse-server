package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.client.DreamAiClient;
import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.dreamuniverse_server.dream.dto.request.DreamCreateRequest;
import com.dreamuniverse_server.dream.dto.request.DreamUpdateRequest;
import com.dreamuniverse_server.dream.dto.response.DreamResponse;
import com.dreamuniverse_server.dream.entity.Dream;
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

    private final DreamRepository dreamRepository;
    private final UserRepository userRepository;

    private final DreamCommandService dreamCommandService;

    private final DreamAiClient dreamAiClient;
    private final DreamAiUsageService dreamAiUsageService;
    private final DreamAiCompletionService dreamAiCompletionService;

    public DreamResponse createDream(
            Long userId,
            DreamCreateRequest request
    ) {
        User user = getUser(userId);

        if (dreamRepository.existsByUserIdAndDreamDate(
                userId,
                request.dreamDate()
        )) {
            throw new CustomException(
                    ErrorCode.DREAM_ALREADY_EXISTS
            );
        }

        // Transaction A
        // 꿈 원문 저장
        Dream dream = dreamCommandService.create(
                user,
                request.title(),
                request.content(),
                request.dreamDate()
        );

        if (request.aiRequested()) {
            analyzeDream(
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
        getUser(userId);

        return dreamRepository
                .findAllByUserIdOrderByDreamDateDesc(userId)
                .stream()
                .map(DreamResponse::from)
                .toList();
    }

    public DreamResponse updateDream(
            Long userId,
            Long dreamId,
            DreamUpdateRequest request
    ) {
        // Transaction A
        Dream dream = dreamCommandService.update(
                dreamId,
                userId,
                request.title(),
                request.content()
        );

        if (request.aiRequested()) {
            analyzeDream(
                    dream.getUser(),
                    dream,
                    request.content()
            );
        }

        return DreamResponse.from(dream);
    }

    public void deleteDream(
            Long userId,
            Long dreamId
    ) {
        dreamCommandService.delete(
                dreamId,
                userId
        );
    }

    private void analyzeDream(
            User user,
            Dream dream,
            String content
    ) {

        /*
         * 사용자 현지 날짜 기준으로
         * AI 사용권 1회 선점.
         */
        LocalDate usageDate =
                dreamAiUsageService.acquire(user);

        try {

            /*
             * DB Transaction 없음
             */
            DreamAnalysisResult result =
                    dreamAiClient.analyze(content);

            /*
             * AI 결과 저장 Transaction
             */
            dreamAiCompletionService.complete(
                    dream,
                    result
            );

        } catch (RuntimeException e) {

            try {

                /*
                 * acquire 당시 사용했던 날짜를 그대로 사용.
                 *
                 * 처리 중 자정을 넘겨도
                 * 잘못된 날짜의 count를 감소시키지 않는다.
                 */
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

        return userRepository.findById(userId)
                .filter(User::isSignupCompleted)
                .orElseThrow(() ->
                        new CustomException(
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
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DREAM_NOT_FOUND
                        )
                );
    }
}