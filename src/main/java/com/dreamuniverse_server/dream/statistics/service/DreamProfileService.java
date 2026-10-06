package com.dreamuniverse_server.dream.statistics.service;

import com.dreamuniverse_server.dream.statistics.dto.response.DreamDnaResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamProfileResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamTypeResponse;
import com.dreamuniverse_server.dream.statistics.type.DreamType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DreamProfileService {

    private final DreamDnaService dreamDnaService;
    private final DreamTypeService dreamTypeService;

    @Value("${dream-assets.base-url}")
    private String dreamAssetsBaseUrl;

    public DreamProfileResponse getProfile(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        DreamDnaResponse dna =
                dreamDnaService.calculate(
                        userId,
                        startDate,
                        endDate
                );

        DreamType dreamType =
                dreamTypeService.classify(
                        dna
                );

        return new DreamProfileResponse(
                DreamTypeResponse.from(
                        dreamType,
                        dreamAssetsBaseUrl
                ),
                dna
        );
    }
}