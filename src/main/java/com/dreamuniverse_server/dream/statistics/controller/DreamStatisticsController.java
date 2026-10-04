package com.dreamuniverse_server.dream.statistics.controller;

import com.dreamuniverse_server.dream.statistics.dto.response.DreamRecentStatisticsResponse;
import com.dreamuniverse_server.dream.statistics.dto.response.DreamYearlyStatisticsResponse;
import com.dreamuniverse_server.dream.statistics.service.DreamStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dreams/statistics")
public class DreamStatisticsController {

    private final DreamStatisticsService dreamStatisticsService;


    @GetMapping("/recent")
    public ResponseEntity<DreamRecentStatisticsResponse>
    getRecentStatistics(
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId =
                Long.valueOf(
                        jwt.getSubject()
                );

        return ResponseEntity.ok(
                dreamStatisticsService
                        .getRecentStatistics(
                                userId
                        )
        );
    }


    @GetMapping("/yearly")
    public ResponseEntity<DreamYearlyStatisticsResponse>
    getYearlyStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam int year
    ) {

        Long userId =
                Long.valueOf(
                        jwt.getSubject()
                );

        return ResponseEntity.ok(
                dreamStatisticsService
                        .getYearlyStatistics(
                                userId,
                                year
                        )
        );
    }
}