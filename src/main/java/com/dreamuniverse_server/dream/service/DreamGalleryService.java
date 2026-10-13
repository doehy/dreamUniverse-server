package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.response.DreamGalleryImageResponse;
import com.dreamuniverse_server.dream.dto.response.DreamGalleryResponse;
import com.dreamuniverse_server.dream.entity.Dream;
import com.dreamuniverse_server.dream.entity.DreamAiStatus;
import com.dreamuniverse_server.dream.entity.DreamImage;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import com.dreamuniverse_server.image.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DreamGalleryService {

    private static final int DEFAULT_SIZE = 6;
    private static final int MAX_SIZE = 20;

    private final DreamImageRepository dreamImageRepository;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public DreamGalleryResponse getGallery(
            Long userId,
            Long cursor,
            Integer requestedSize
    ) {
        int size = normalizeSize(
                requestedSize
        );

        PageRequest pageable =
                PageRequest.of(
                        0,
                        size + 1
                );

        List<DreamImage> images;

        if (cursor == null) {
            images =
                    dreamImageRepository
                            .findFirstGalleryImages(
                                    userId,
                                    DreamAiStatus.COMPLETED,
                                    pageable
                            );
        } else {
            images =
                    dreamImageRepository
                            .findNextGalleryImages(
                                    userId,
                                    DreamAiStatus.COMPLETED,
                                    cursor,
                                    pageable
                            );
        }

        boolean hasNext =
                images.size() > size;

        List<DreamImage> currentImages =
                hasNext
                        ? images.subList(
                        0,
                        size
                )
                        : images;

        List<DreamGalleryImageResponse> responses =
                currentImages
                        .stream()
                        .map(
                                this::toResponse
                        )
                        .toList();

        Long nextCursor =
                hasNext
                        ? currentImages
                        .getLast()
                        .getId()
                        : null;

        return new DreamGalleryResponse(
                responses,
                nextCursor,
                hasNext
        );
    }

    private DreamGalleryImageResponse toResponse(
            DreamImage image
    ) {
        Dream dream = image.getDream();

        String imageKey = image.getImageKey();

        String imageUrl =
                imageStorageService.generatePresignedGetUrl(
                        imageKey
                );

        return new DreamGalleryImageResponse(
                dream.getId(),
                dream.getDreamDate(),
                imageKey,
                imageUrl
        );
    }

    private int normalizeSize(
            Integer requestedSize
    ) {
        if (
                requestedSize == null
                        || requestedSize <= 0
        ) {
            return DEFAULT_SIZE;
        }

        return Math.min(
                requestedSize,
                MAX_SIZE
        );
    }
}