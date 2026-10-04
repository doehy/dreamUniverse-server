package com.dreamuniverse_server.dream.statistics.repository.projection;

public interface DreamStatisticsElementProjection {

    String getElementType();

    String getElementValue();

    Long getAppearanceCount();
}