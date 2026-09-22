package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamImage;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DreamImagePersistenceService {

    private final DreamImageRepository dreamImageRepository;

    @Transactional
    public List<String> replace(
            Dream dream,
            String imageKey
    ) {
        List<String> oldImageKeys =
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(
                                dream.getId()
                        )
                        .stream()
                        .map(
                                DreamImage::getImageKey
                        )
                        .toList();

        dreamImageRepository.deleteByDreamId(
                dream.getId()
        );

        DreamImage dreamImage =
                DreamImage.create(
                        dream,
                        imageKey,
                        1
                );

        dreamImageRepository.save(
                dreamImage
        );

        return oldImageKeys;
    }
}