package com.dreamuniverse_server.dream.statistics.dto.response;

import java.util.List;

public record DreamMonthlyEmotionTrendResponse(

        int month,

        String dominantEmotion,

        List<DreamEmotionStatisticsResponse> emotions

) {
}