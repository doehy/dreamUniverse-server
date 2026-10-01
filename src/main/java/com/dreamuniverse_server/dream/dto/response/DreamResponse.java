package com.dreamuniverse_server.dream.dto.response;

import com.dreamuniverse_server.dream.entity.Dream;

import java.time.Instant;
import java.time.LocalDate;

public record DreamResponse(

        Long dreamId,

        String title,

        String content,

        LocalDate dreamDate,

        boolean sleepParalysis,

        boolean nightmare,

        boolean lucidDream,

        String aiStatus,

        Instant createdAt,

        Instant updatedAt

) {

    public static DreamResponse from(
            Dream dream
    ) {
        return new DreamResponse(
                dream.getId(),
                dream.getTitle(),
                dream.getContent(),
                dream.getDreamDate(),
                dream.isSleepParalysis(),
                dream.isNightmare(),
                dream.isLucidDream(),
                dream.getAiStatus().name(),
                dream.getCreatedAt(),
                dream.getUpdatedAt()
        );
    }
}