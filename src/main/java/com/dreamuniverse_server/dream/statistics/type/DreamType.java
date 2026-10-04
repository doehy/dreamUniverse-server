package com.dreamuniverse_server.dream.statistics.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DreamType {

    EMOTION_VOYAGER(
            "감정의 항해자",
            "감정의 움직임이 크고, 꿈속에서 느낀 감정이 강하게 기억되는 유형이에요.",
            100, 40, 30, 40, 55, 50, 50, 45
    ),

    QUIET_OBSERVER(
            "고요한 관찰자",
            "강한 사건보다는 차분한 장면과 분위기를 바라보듯 경험하는 꿈이 많은 유형이에요.",
            25, 10, 35, 45, 40, 30, 15, 20
    ),

    NIGHTMARE_WANDERER(
            "악몽을 헤매는 방랑자",
            "긴장감이 높은 꿈과 악몽을 자주 지나며, 불안하고 기묘한 세계를 경험하는 유형이에요.",
            70, 100, 20, 55, 35, 65, 45, 80
    ),

    STORM_CHASER(
            "폭풍 속 추격자",
            "강한 긴장감 속에서 사건과 장면이 빠르게 바뀌는 역동적인 꿈을 꾸는 유형이에요.",
            80, 90, 20, 20, 60, 30, 100, 40
    ),

    RECURRING_WORLD_RESIDENT(
            "반복 세계의 주민",
            "비슷한 장소와 요소가 여러 꿈에 다시 등장하며 자신만의 꿈 세계가 이어지는 유형이에요.",
            50, 35, 20, 100, 45, 60, 25, 70
    ),

    MEMORY_RETURNER(
            "기억의 회귀자",
            "가족, 친구, 학교처럼 익숙한 사람과 장소가 반복해서 꿈에 나타나는 유형이에요.",
            55, 30, 15, 100, 75, 30, 20, 15
    ),

    STAR_LUCID_DREAMER(
            "별빛 자각자",
            "꿈이라는 사실을 알아차리는 경험이 많고, 비현실적인 세계를 의식적으로 바라보는 유형이에요.",
            55, 20, 100, 30, 35, 60, 50, 85
    ),

    DREAM_DIRECTOR(
            "꿈속의 감독관",
            "꿈을 알아차리거나 상황에 적극적으로 개입하며 꿈의 흐름을 바라보는 유형이에요.",
            40, 15, 100, 35, 45, 30, 75, 40
    ),

    PEOPLE_STORYTELLER(
            "사람 사이를 걷는 이야기꾼",
            "꿈의 중심에 사람과 관계가 자주 등장하고, 인물 사이의 이야기가 중요한 유형이에요.",
            65, 35, 25, 45, 100, 25, 50, 25
    ),

    LONELY_WORLD_WANDERER(
            "고독한 세계의 방랑자",
            "사람보다는 낯선 공간과 세계를 홀로 돌아다니는 꿈이 많은 유형이에요.",
            50, 25, 25, 30, 0, 50, 60, 75
    ),

    SYMBOL_COLLECTOR(
            "상징을 줍는 수집가",
            "평범한 장면보다 의미심장한 사물과 이미지가 꿈속에서 강하게 나타나는 유형이에요.",
            55, 40, 25, 50, 25, 100, 30, 65
    ),

    REALITY_ECHO(
            "현실의 메아리",
            "실제 생활과 가까운 사람, 장소, 상황이 꿈에서도 반복해서 이어지는 유형이에요.",
            45, 25, 20, 60, 65, 15, 30, 0
    ),

    DIMENSION_WANDERER(
            "차원을 넘는 방랑자",
            "현실에서 보기 어려운 세계를 넘나들며 끊임없이 새로운 장면을 경험하는 유형이에요.",
            60, 30, 40, 15, 20, 60, 90, 100
    ),

    CHAOS_SHIFTER(
            "혼돈의 변신가",
            "장소와 사건, 분위기가 끊임없이 변하며 예측하기 어려운 꿈을 자주 꾸는 유형이에요.",
            70, 55, 30, 10, 40, 65, 100, 90
    ),

    PEACEFUL_TRAVELER(
            "평온한 꿈 여행자",
            "긴장감은 낮고 편안한 분위기 속에서 자연스럽게 꿈을 여행하는 유형이에요.",
            40, 0, 30, 30, 50, 35, 50, 40
    ),

    REALITY_EXPLORER(
            "현실의 탐험가",
            "현실적인 세계 안에서 다양한 장소와 사건을 빠르게 경험하는 유형이에요.",
            50, 25, 25, 15, 50, 15, 100, 0
    );

    private final String displayName;
    private final String description;

    private final int emotionIntensity;
    private final int tension;
    private final int lucidity;
    private final int recurrence;
    private final int relationship;
    private final int symbolism;
    private final int dynamism;
    private final int fantasy;
}