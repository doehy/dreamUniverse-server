package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamImage;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DreamImagePersistenceService {

    private final DreamImageRepository dreamImageRepository;

    @Transactional
    public DreamImage replace(
            Dream dream,
            String imageKey
    ) {
        dreamImageRepository.deleteByDreamId(
                dream.getId()
        );

        DreamImage dreamImage =
                DreamImage.create(
                        dream,
                        imageKey,
                        1
                );

        return dreamImageRepository.save(
                dreamImage
        );
    }
}