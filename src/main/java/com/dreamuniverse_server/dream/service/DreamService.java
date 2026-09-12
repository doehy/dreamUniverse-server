package com.dreamuniverse_server.dream.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DreamService {

    private final DreamRepository dreamRepository;
    private final UserRepository userRepository;

    public DreamResponse createDream(Long userId, DreamCreateRequest request) {
        User user = getUser(userId);

        if (dreamRepository.existsByUserIdAndDreamDate(userId, request.dreamDate())) {
            throw new CustomException(ErrorCode.DREAM_ALREADY_EXISTS);
        }

        Dream dream = Dream.create(user, request.content(), request.dreamDate());

        dreamRepository.save(dream);

        if (request.aiRequested()) {
            // TODO AI 분석/그림 생성
        }

        return DreamResponse.from(dream);
    }

    @Transactional(readOnly = true)
    public DreamResponse getDream(Long userId, Long dreamId) {
        Dream dream = getDreamByUser(dreamId, userId);
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

    public DreamResponse updateDream(Long userId, Long dreamId, DreamUpdateRequest request) {
        Dream dream = getDreamByUser(dreamId, userId);

        dream.update(request.content());

        if (request.aiRequested()) {
            // TODO AI 재분석/그림 재생성
        }
        return DreamResponse.from(dream);
    }

    public void deleteDream(Long userId, Long dreamId) {
        Dream dream = getDreamByUser(dreamId, userId);

        dreamRepository.delete(dream);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isSignupCompleted)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private Dream getDreamByUser(Long dreamId, Long userId) {
        return dreamRepository
                .findByIdAndUserId(
                        dreamId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(ErrorCode.DREAM_NOT_FOUND)
                );
    }
}