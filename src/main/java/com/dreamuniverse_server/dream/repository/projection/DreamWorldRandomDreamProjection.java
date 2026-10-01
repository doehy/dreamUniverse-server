package com.dreamuniverse_server.dream.repository.projection;

import java.time.LocalDate;

public interface DreamWorldRandomDreamProjection {

    Long getDreamId();

    LocalDate getDreamDate();

    String getTitle();
}