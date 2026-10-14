package com.dreamuniverse_server.ad.entity;

public enum RewardedAdStatus {
    PENDING, // 광고 시청 세션만 발급된 상태
    VERIFIED, // Google SSV 검증이 완료된 상태
    CONSUMED // AI 분석 요청에 보상 1회를 사용한 상태
}
