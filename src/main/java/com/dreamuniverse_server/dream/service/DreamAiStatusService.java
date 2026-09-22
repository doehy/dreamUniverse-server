package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.dreamuniverse_server.global.exception.ErrorCode.DREAM_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class DreamAiStatusService {

    private final DreamRepository dreamRepository;

    @Transactional
    public void markProcessing(
            Long dreamId
    ) {
        Dream dream =
                getDream(dreamId);

        dream.startAiCompletion();
    }

    @Transactional
    public void markFailed(
            Long dreamId
    ) {
        Dream dream =
                getDream(dreamId);

        dream.failAiCompletion();
    }

    private Dream getDream(
            Long dreamId
    ) {
        return dreamRepository.findById(dreamId)
                .orElseThrow(
                        () -> new CustomException(
                                DREAM_NOT_FOUND
                        )
                );
    }
}