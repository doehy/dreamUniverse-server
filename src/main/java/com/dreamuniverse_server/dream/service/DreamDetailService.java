package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.dream.dto.response.DreamAnalysisResponse;
import com.dreamuniverse_server.dream.dto.response.DreamDetailImageResponse;
import com.dreamuniverse_server.dream.dto.response.DreamDetailResponse;
import com.dreamuniverse_server.dream.dto.response.DreamResponse;
import com.dreamuniverse_server.dream.entity.DreamAnalysis;
import com.dreamuniverse_server.dream.repository.DreamAnalysisRepository;
import com.dreamuniverse_server.dream.repository.DreamElementRepository;
import com.dreamuniverse_server.dream.repository.DreamEmotionRepository;
import com.dreamuniverse_server.dream.repository.DreamImageRepository;
import com.dreamuniverse_server.image.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DreamDetailService {

    private final DreamService dreamService;

    private final DreamAnalysisRepository dreamAnalysisRepository;
    private final DreamEmotionRepository dreamEmotionRepository;
    private final DreamElementRepository dreamElementRepository;
    private final DreamImageRepository dreamImageRepository;

    private final ImageStorageService imageStorageService;


    @Transactional(readOnly = true)
    public DreamDetailResponse getDetail(
            Long userId,
            Long dreamId
    ) {
        /*
         * 1. 기존 꿈 단건 조회를 이용해서
         *    존재 여부 + 본인 소유 여부를 먼저 검증한다.
         */
        DreamResponse dream =
                dreamService.getDream(
                        userId,
                        dreamId
                );


        /*
         * 2. 이미지 조회
         */
        List<DreamDetailImageResponse> images =
                dreamImageRepository
                        .findAllByDreamIdOrderByImageOrderAsc(
                                dreamId
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


        /*
         * 3. 분석 조회
         *
         * AI 생성 중이거나 AI를 요청하지 않은 꿈이면
         * 분석이 아직 없을 수 있으므로 null을 반환한다.
         */
        DreamAnalysisResponse analysis =
                dreamAnalysisRepository
                        .findByDreamId(
                                dreamId
                        )
                        .map(this::toAnalysisResponse)
                        .orElse(null);


        return new DreamDetailResponse(
                dream,
                images,
                analysis
        );
    }


    private DreamAnalysisResponse toAnalysisResponse(
            DreamAnalysis analysis
    ) {
        return DreamAnalysisResponse.from(
                analysis,
                dreamEmotionRepository
                        .findAllByAnalysisId(
                                analysis.getId()
                        ),
                dreamElementRepository
                        .findAllByAnalysisId(
                                analysis.getId()
                        )
        );
    }
}