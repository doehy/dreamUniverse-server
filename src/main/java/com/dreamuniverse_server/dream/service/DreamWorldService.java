package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.response.DreamDetailImageResponse;
import com.dreamuniverse_server.dream.dto.response.DreamWorldElementResponse;
import com.dreamuniverse_server.dream.dto.response.DreamWorldRandomDreamResponse;
import com.dreamuniverse_server.dream.dto.response.DreamWorldRandomElementResponse;
import com.dreamuniverse_server.dream.dto.response.DreamWorldRandomResponse;
import com.dreamuniverse_server.dream.dto.response.DreamWorldResponse;
import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldElementProjection;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldRandomDreamProjection;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldRandomElementProjection;
import com.dreamuniverse_server.image.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DreamWorldService {

    private static final int MAX_ELEMENTS_PER_TYPE = 5;
    private static final int RANDOM_ELEMENT_COUNT = 12;

    private final DreamElementRepository dreamElementRepository;
    private final DreamImageRepository dreamImageRepository;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public DreamWorldResponse getWorld(Long userId) {

        List<DreamWorldElementResponse> people =
                getTopElements(userId, DreamElementType.PERSON);

        List<DreamWorldElementResponse> places =
                getTopElements(userId, DreamElementType.PLACE);

        List<DreamWorldElementResponse> symbols =
                getTopElements(userId, DreamElementType.OBJECT);

        return new DreamWorldResponse(
                people,
                places,
                symbols
        );
    }

    @Transactional(readOnly = true)
    public DreamWorldRandomResponse getRandomWorld(Long userId) {

        List<DreamWorldRandomElementResponse> elements =
                dreamElementRepository
                        .findRandomElements(
                                userId,
                                PageRequest.of(
                                        0,
                                        RANDOM_ELEMENT_COUNT
                                )
                        )
                        .stream()
                        .map(this::toRandomResponse)
                        .toList();

        return new DreamWorldRandomResponse(elements);
    }

    @Transactional(readOnly = true)
    public DreamWorldRandomDreamResponse getRandomDream(
            Long userId,
            DreamElementType elementType,
            String elementValue,
            Long excludeDreamId
    ) {

        DreamWorldRandomDreamProjection dream =
                dreamElementRepository
                        .findRandomDreamByElement(
                                userId,
                                elementType.name(),
                                elementValue,
                                excludeDreamId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 요소가 포함된 꿈을 찾을 수 없습니다."
                                )
                        );

        List<DreamDetailImageResponse> images =
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(
                                dream.getDreamId()
                        )
                        .stream()
                        .map(image ->
                                new DreamDetailImageResponse(
                                        image.getImageOrder(),
                                        imageStorageService
                                                .generatePresignedGetUrl(
                                                        image.getImageKey()
                                                )
                                )
                        )
                        .toList();

        return new DreamWorldRandomDreamResponse(
                dream.getDreamId(),
                dream.getDreamDate(),
                dream.getTitle(),
                images
        );
    }

    private List<DreamWorldElementResponse> getTopElements(
            Long userId,
            DreamElementType elementType
    ) {

        return dreamElementRepository
                .findTopElements(
                        userId,
                        elementType,
                        PageRequest.of(
                                0,
                                MAX_ELEMENTS_PER_TYPE
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DreamWorldElementResponse toResponse(
            DreamWorldElementProjection projection
    ) {

        return DreamWorldElementResponse.of(
                projection.getElementValue(),
                projection.getAppearanceCount()
        );
    }

    private DreamWorldRandomElementResponse toRandomResponse(
            DreamWorldRandomElementProjection projection
    ) {

        return DreamWorldRandomElementResponse.of(
                DreamElementType.valueOf(
                        projection.getElementType()
                ),
                projection.getElementValue(),
                projection.getAppearanceCount()
        );
    }
}