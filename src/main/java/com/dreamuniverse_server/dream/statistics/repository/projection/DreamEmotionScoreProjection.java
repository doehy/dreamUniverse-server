package com.dreamuniverse_server.dream.statistics.repository.projection;

public interface DreamEmotionScoreProjection {

    String getEmotionType();

    Long getTotalScore();
}