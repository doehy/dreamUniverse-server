package com.dreamuniverse_server.dream.statistics.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DreamRecentStatisticsResponse(

        LocalDate startDate,

        LocalDate endDate,

        long dreamCount,

        DreamExperienceStatisticsResponse sleepParalysis,

        DreamExperienceStatisticsResponse nightmare,

        DreamExperienceStatisticsResponse lucidDream,

        String dominantEmotion,

        List<DreamEmotionStatisticsResponse> emotions,

        List<DreamStatisticsElementResponse> topElements

) {
}