package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.response.DreamWorldElementResponse;
import com.dreamuniverse_server.dream.dto.response.DreamWorldResponse;
import com.dreamuniverse_server.dream.entity.DreamElementType;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.projection.DreamWorldElementProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DreamWorldService {

    private static final int MAX_ELEMENTS_PER_TYPE = 5;

    private final DreamElementRepository dreamElementRepository;

    @Transactional(readOnly = true)
    public DreamWorldResponse getWorld(Long userId) {

        List<DreamWorldElementResponse> people =
                getTopElements(
                        userId,
                        DreamElementType.PERSON
                );

        List<DreamWorldElementResponse> places =
                getTopElements(
                        userId,
                        DreamElementType.PLACE
                );

        List<DreamWorldElementResponse> symbols =
                getTopElements(
                        userId,
                        DreamElementType.OBJECT
                );

        return new DreamWorldResponse(
                people,
                places,
                symbols
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
                        PageRequest.of(0, MAX_ELEMENTS_PER_TYPE)
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
}