package com.dreamuniverse_server.dream.statistics.repository.projection;

public interface DreamMonthlyEmotionScoreProjection {

    Integer getMonth();

    String getEmotionType();

    Long getTotalScore();
}