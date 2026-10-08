package com.dreamuniverse_server.subscription.entity;

public enum SubscriptionStatus {

    ACTIVE, //현재 구독 중
    CANCELED, //자동 갱신은 취소했지만 이미 결제한 기간은 아직 남아 있을 수 있음
    EXPIRED //구독 기간 완전히 종료
}