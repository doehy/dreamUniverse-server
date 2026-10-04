package com.dreamuniverse_server.dream.statistics.repository.projection;

public interface DreamExperienceStatisticsProjection {

    Long getDreamCount();

    Long getSleepParalysisCount();

    Long getNightmareCount();

    Long getLucidDreamCount();
}