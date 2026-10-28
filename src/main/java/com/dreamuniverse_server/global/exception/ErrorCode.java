package com.dreamuniverse_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Global
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "GLOBAL_400_1", "입력값이 유효하지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL_500_1", "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GLOBAL_401_1", "인증되지 않은 사용자입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "GLOBAL_403_1", "허용되지 않은 요청입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "유저를 찾을 수 없습니다."),

    USER_DELETE_WHILE_PROCESSING(HttpStatus.CONFLICT, "USER_409_1", "꿈 분석이 진행 중이에요. 완료 후 회원 탈퇴를 다시 요청해 주세요."),
    DREAM_IMAGE_DELETE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "DREAM_503_1", "이미지 삭제를 완료하지 못했어요. 잠시 후 다시 삭제해 주세요."),
    USER_IMAGE_DELETE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "USER_503_1", "이미지 삭제를 완료하지 못했어요. 회원 탈퇴가 완료되지 않았으니 잠시 후 다시 요청해 주세요."),

    // Ad Reward
    AD_REWARD_REQUIRED(HttpStatus.BAD_REQUEST, "AD_400_1", "AI 분석을 위해 광고 시청이 필요합니다."),
    AD_REWARD_NOT_AVAILABLE(HttpStatus.CONFLICT, "AD_409_1", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    // Dream
    DREAM_NOT_FOUND(HttpStatus.NOT_FOUND, "DREAM_404_1", "꿈을 찾을 수 없습니다."),
    DREAM_FORBIDDEN(HttpStatus.FORBIDDEN, "DREAM_403_1", "해당 꿈에 접근할 권한이 없습니다."),
    DREAM_AI_PROCESSING(HttpStatus.CONFLICT, "DREAM_409_1", "분석 중에는 삭제할 수 없습니다. 완료 후 삭제해 주세요."),
    DREAM_AI_ALREADY_PROCESSING(HttpStatus.CONFLICT, "DREAM_409_2", "이미 분석이 진행 중입니다. 완료 후 다시 시도해 주세요."),
    DREAM_UPDATE_WHILE_PROCESSING(HttpStatus.CONFLICT, "DREAM_409_3", "분석 중에는 수정할 수 없습니다. 완료 후 수정해 주세요."),
    DREAM_AI_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,"DREAM_429_1","오늘 사용할 수 있는 AI 완성 횟수를 모두 사용했습니다."),
    DREAM_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
            "DREAM_429_2", "하루에 작성할 수 있는 꿈은 최대 3개입니다."),
    DREAM_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND,"DREAM_404_2","꿈 분석 결과를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}