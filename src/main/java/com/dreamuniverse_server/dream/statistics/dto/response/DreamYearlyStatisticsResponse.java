package com.dreamuniverse_server.dream.statistics.dto.response;

import java.util.List;

public record DreamYearlyStatisticsResponse(

        int year,

        long dreamCount,

        DreamExperienceStatisticsResponse sleepParalysis,

        DreamExperienceStatisticsResponse nightmare,

        DreamExperienceStatisticsResponse lucidDream,

        String dominantEmotion,

        List<DreamEmotionStatisticsResponse> emotions,

        List<DreamStatisticsElementResponse> topElements,

        List<DreamMonthlyEmotionTrendResponse> monthlyEmotionTrend

) {
}