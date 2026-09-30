package com.dreamuniverse_server.dream.controller;

import com.dreamuniverse_server.dream.dto.request.DreamCreateRequest;
import com.dreamuniverse_server.dream.dto.request.DreamUpdateRequest;
import com.dreamuniverse_server.dream.dto.response.*;
import com.dreamuniverse_server.dream.service.DreamDetailService;
import com.dreamuniverse_server.dream.service.DreamGalleryService;
import com.dreamuniverse_server.dream.service.DreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dreams")
public class DreamController {

    private final DreamService dreamService;
    private final DreamGalleryService dreamGalleryService;
    private final DreamDetailService dreamDetailService;

    /**
     * 꿈 작성
     */
    @PostMapping
    public ResponseEntity<DreamResponse> createDream(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DreamCreateRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());

        DreamResponse response =
                dreamService.createDream(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 꿈 단건 조회
     */
    @GetMapping("/{dreamId}")
    public ResponseEntity<DreamResponse> getDream(@AuthenticationPrincipal Jwt jwt, @PathVariable Long dreamId) {
        Long userId = Long.valueOf(jwt.getSubject());

        DreamResponse response =
                dreamService.getDream(userId, dreamId);

        return ResponseEntity.ok(response);
    }

    /**
     * 내가 작성한 꿈 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<DreamResponse>> getDreams(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());

        List<DreamResponse> response =
                dreamService.getDreams(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 꿈 수정
     */
    @PatchMapping("/{dreamId}")
    public ResponseEntity<DreamResponse> updateDream(@AuthenticationPrincipal Jwt jwt, @PathVariable Long dreamId,
            @Valid @RequestBody DreamUpdateRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        DreamResponse response =
                dreamService.updateDream(
                        userId,
                        dreamId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 꿈 삭제
     */
    @DeleteMapping("/{dreamId}")
    public ResponseEntity<Void> deleteDream(@AuthenticationPrincipal Jwt jwt, @PathVariable Long dreamId) {
        Long userId = Long.valueOf(jwt.getSubject());

        dreamService.deleteDream(userId, dreamId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Ai 분석 사용량 확인
     */
    @GetMapping("/ai-usage")
    public ResponseEntity<DreamAiUsageResponse> getAiUsage(
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId =
                Long.valueOf(
                        jwt.getSubject()
                );

        return ResponseEntity.ok(
                dreamService.getAiUsage(
                        userId
                )
        );
    }

    /**
     * 꿈 분석 상세 조회
     */
    @GetMapping("/{dreamId}/analysis")
    public ResponseEntity<DreamAnalysisResponse> getAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long dreamId
    ) {

        Long userId =
                Long.valueOf(
                        jwt.getSubject()
                );

        return ResponseEntity.ok(
                dreamService.getAnalysis(
                        userId,
                        dreamId
                )
        );
    }

    /**
     * 꿈그림 커서 방식으로 조회
     */
    @GetMapping("/gallery")
    public ResponseEntity<DreamGalleryResponse> getGallery(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        Long userId =
                Long.valueOf(
                        jwt.getSubject()
                );

        return ResponseEntity.ok(
                dreamGalleryService.getGallery(
                        userId,
                        cursor,
                        size
                )
        );
    }

    /**
     * 꿈 상세화면 조회
     */
    @GetMapping("/{dreamId}/detail")
    public ResponseEntity<DreamDetailResponse> getDreamDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long dreamId
    ) {
        Long userId =
                Long.valueOf(
                        jwt.getSubject()
                );

        return ResponseEntity.ok(
                dreamDetailService.getDetail(
                        userId,
                        dreamId
                )
        );
    }
}