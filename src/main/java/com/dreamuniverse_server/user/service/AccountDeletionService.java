package com.dreamuniverse_server.user.service;

import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import com.dreamuniverse_server.dream.repository.DreamRepository;
import com.dreamuniverse_server.dream.service.DreamImageService;
import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.repository.AccountDeletionRepository;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountDeletionService {
    private final UserRepository userRepository;
    private final DreamRepository dreamRepository;
    private final DreamImageRepository dreamImageRepository;
    private final DreamImageService dreamImageService;
    private final AccountDeletionRepository accountDeletionRepository;

    public void delete(Long userId) {
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        var dreams = dreamRepository.findAllByUserIdForUpdate(userId);
        if (dreams.stream().anyMatch(dream -> dream.getAiStatus() == DreamAiStatus.PROCESSING)) {
            throw new CustomException(ErrorCode.USER_DELETE_WHILE_PROCESSING);
        }
        // Keep DB keys until all R2 deletes succeed. Repeating deletes of absent objects is safe.
        for (var dream : dreams) {
            var keys = dreamImageRepository.findAllByDreamIdOrderByImageOrderAsc(dream.getId())
                    .stream().map(image -> image.getImageKey()).toList();
            try {
                dreamImageService.deleteDreamImagesStrict(dream.getId(), keys);
            } catch (RuntimeException error) {
                throw new CustomException(ErrorCode.USER_IMAGE_DELETE_FAILED);
            }
        }
        accountDeletionRepository.deleteRelatedData(userId, user.getProvider().name(), user.getProviderUserId());
    }
}
