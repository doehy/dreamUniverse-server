package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DreamCommandService {

    private final DreamRepository dreamRepository;

    @Transactional
    public Dream create(
            User user,
            String title,
            String content,
            LocalDate dreamDate,
            boolean sleepParalysis,
            boolean nightmare,
            boolean lucidDream
    ) {
        Dream dream = Dream.create(
                user,
                title,
                content,
                dreamDate,
                sleepParalysis,
                nightmare,
                lucidDream
        );

        return dreamRepository.save(dream);
    }

    @Transactional
    public Dream update(
            Long dreamId,
            Long userId,
            String title,
            String content,
            boolean sleepParalysis,
            boolean nightmare,
            boolean lucidDream
    ) {
        Dream dream = dreamRepository
                .findByIdAndUserId(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DREAM_NOT_FOUND
                        )
                );

        dream.update(
                title,
                content,
                sleepParalysis,
                nightmare,
                lucidDream
        );

        return dream;
    }

    @Transactional
    public void delete(
            Long dreamId,
            Long userId
    ) {
        Dream dream = dreamRepository
                .findByIdAndUserId(dreamId, userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DREAM_NOT_FOUND
                        )
                );

        dreamRepository.delete(dream);
    }
}