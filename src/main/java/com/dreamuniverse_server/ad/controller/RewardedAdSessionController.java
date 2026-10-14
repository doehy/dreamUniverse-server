
package com.dreamuniverse_server.ad.controller;

import com.dreamuniverse_server.ad.entity.RewardedAdSession;
import com.dreamuniverse_server.ad.service.RewardedAdSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ads/rewarded/sessions")
public class RewardedAdSessionController {

    private final RewardedAdSessionService sessionService;

    /**
     * 광고 시청을 위한 세션 생성
     */
    @PostMapping
    public ResponseEntity<CreateResponse> create(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        RewardedAdSession session =
                sessionService.create(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateResponse(
                        session.getId(),
                        session.getVerificationExpiresAt()
                ));
    }

    /**
     * 광고 검증 상태 조회
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<StatusResponse> getStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sessionId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        RewardedAdSession session =
                sessionService.get(userId, sessionId);

        return ResponseEntity.ok(
                new StatusResponse(
                        session.getId(),
                        session.getStatus().name(),
                        session.getVerificationExpiresAt(),
                        session.getRewardExpiresAt()
                )
        );
    }

    /**
     * 광고 세션 생성 응답
     */
    public record CreateResponse(
            String sessionId,
            Instant verificationExpiresAt
    ) {
    }

    /**
     * 광고 보상 검증 상태 응답
     */
    public record StatusResponse(
            String sessionId,
            String status,
            Instant verificationExpiresAt,
            Instant rewardExpiresAt
    ) {
    }
}
