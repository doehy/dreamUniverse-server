package com.dreamuniverse_server.ad.controller;

import com.dreamuniverse_server.ad.service.AdMobSsvRewardService;
import com.dreamuniverse_server.ad.service.AdMobSsvVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ads/admob")
public class AdMobSsvController {

    private final AdMobSsvVerifier verifier;
    private final AdMobSsvRewardService rewardService;

    @GetMapping("/ssv")
    public ResponseEntity<Void> receive(
            HttpServletRequest request
    ) {
        // 서명된 원본 쿼리 문자열 보존
        String rawQuery = request.getQueryString();

        // Google 전자서명 검증
        AdMobSsvVerifier.VerifiedCallback callback =
                verifier.verify(rawQuery);

        // 검증된 광고 보상 기록
        rewardService.grant(callback);

        return ResponseEntity.ok().build();
    }
}
