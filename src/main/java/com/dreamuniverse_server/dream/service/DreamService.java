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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // Dream 원문 저장 후 바로 COMMIT
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
    public List<DreamResponse> getDreams(Long userId) {

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
        // 제목/내용 수정 후 COMMIT
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
         * 1. API 호출 전에 일일 한도 확인
         *
         * 이미 2/2라면 비싼 OpenAI API를
         * 호출할 이유 자체가 없으므로 먼저 검사
         */
        dreamAiUsageService.validateAvailable(
                user.getId()
        );

        /*
         * 2. 외부 OpenAI 호출
         *
         * 여기서는 DB Transaction이 열려 있지 않음.
         */
        DreamAnalysisResult result =
                dreamAiClient.analyze(content);

        /*
         * 3. Transaction B
         *
         * - DreamAnalysis
         * - DreamEmotion
         * - DreamElement
         * - DreamAiUsage
         *
         * 전부 하나의 Transaction으로 처리
         */
        dreamAiCompletionService.complete(
                user,
                dream,
                result
        );
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