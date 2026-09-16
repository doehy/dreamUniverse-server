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
@Transactional
public class DreamService {

    private final DreamRepository dreamRepository;
    private final UserRepository userRepository;

    private final DreamAiClient dreamAiClient;
    private final DreamAnalysisService dreamAnalysisService;
    private final DreamAiUsageService dreamAiUsageService;

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

        Dream dream = Dream.create(
                user,
                request.title(),
                request.content(),
                request.dreamDate()
        );

        dreamRepository.save(dream);

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
        Dream dream = getDreamByUser(
                dreamId,
                userId
        );

        dream.update(
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
        Dream dream = getDreamByUser(
                dreamId,
                userId
        );

        dreamRepository.delete(dream);
    }

    /**
     * AI 분석 공통 처리
     *
     * 생성/수정에서 똑같은 코드가 반복되기 때문에
     * private 메서드로 분리
     */
    private void analyzeDream(
            User user,
            Dream dream,
            String content
    ) {
        // 1. 오늘 AI를 더 사용할 수 있는지 확인
        dreamAiUsageService.validateAvailable(
                user.getId()
        );

        // 2. 실제 OpenAI 호출
        DreamAnalysisResult result =
                dreamAiClient.analyze(content);

        // 3. 분석 결과 DB 저장
        dreamAnalysisService.saveAnalysis(
                dream,
                result
        );

        // 4. 여기까지 모두 성공했을 때만 사용 횟수 +1
        dreamAiUsageService.use(user);
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