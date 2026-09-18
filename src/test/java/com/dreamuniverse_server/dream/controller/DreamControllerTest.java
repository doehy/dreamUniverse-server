package com.dreamuniverse_server.dream.controller;

import com.dreamuniverse_server.dream.dto.response.DreamAiUsageResponse;
import com.dreamuniverse_server.dream.dto.response.DreamAnalysisResponse;
import com.dreamuniverse_server.dream.dto.response.DreamElementResponse;
import com.dreamuniverse_server.dream.dto.response.DreamEmotionResponse;
import com.dreamuniverse_server.dream.service.DreamService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DreamController.class)
class DreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DreamService dreamService;

    /*
     * @WebMvcTest에서는 JPA Entity를 로딩하지 않지만
     * 프로젝트의 @EnableJpaAuditing 설정은 적용될 수 있다.
     *
     * 따라서 JPA Auditing이 요구하는 MappingContext를
     * mock으로 제공한다.
     */
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;


    @Test
    void AI_남은_사용횟수를_조회한다()
            throws Exception {

        // given
        Long userId = 1L;

        DreamAiUsageResponse response =
                new DreamAiUsageResponse(
                        1,
                        2
                );

        given(
                dreamService.getAiUsage(userId)
        )
                .willReturn(response);


        // when & then
        mockMvc.perform(
                        get("/dreams/ai-usage")
                                .with(
                                        jwt()
                                                .jwt(
                                                        jwt -> jwt
                                                                .subject(
                                                                        userId.toString()
                                                                )
                                                )
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.remainingCount")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.dailyLimit")
                                .value(2)
                );


        verify(
                dreamService
        )
                .getAiUsage(userId);
    }

    @Test
    void 꿈의_AI_분석결과를_조회한다()
            throws Exception {

        // given
        Long userId = 1L;
        Long dreamId = 10L;

        DreamAnalysisResponse response =
                new DreamAnalysisResponse(
                        "시험에 대한 압박감이 반영된 꿈입니다.",
                        List.of(
                                new DreamEmotionResponse(
                                        "ANXIETY",
                                        80
                                ),
                                new DreamEmotionResponse(
                                        "CONFUSION",
                                        55
                                )
                        ),
                        List.of(
                                new DreamElementResponse(
                                        "PLACE",
                                        "학교"
                                ),
                                new DreamElementResponse(
                                        "EVENT",
                                        "시험"
                                )
                        )
                );

        given(
                dreamService.getAnalysis(
                        userId,
                        dreamId
                )
        )
                .willReturn(response);


        // when & then
        mockMvc.perform(
                        get(
                                "/dreams/{dreamId}/analysis",
                                dreamId
                        )
                                .with(
                                        jwt()
                                                .jwt(
                                                        jwt -> jwt.subject(
                                                                userId.toString()
                                                        )
                                                )
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.interpretation")
                                .value(
                                        "시험에 대한 압박감이 반영된 꿈입니다."
                                )
                )

                .andExpect(
                        jsonPath("$.emotions.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.emotions[0].type")
                                .value("ANXIETY")
                )
                .andExpect(
                        jsonPath("$.emotions[0].score")
                                .value(80)
                )
                .andExpect(
                        jsonPath("$.emotions[1].type")
                                .value("CONFUSION")
                )
                .andExpect(
                        jsonPath("$.emotions[1].score")
                                .value(55)
                )

                .andExpect(
                        jsonPath("$.elements.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.elements[0].type")
                                .value("PLACE")
                )
                .andExpect(
                        jsonPath("$.elements[0].value")
                                .value("학교")
                )
                .andExpect(
                        jsonPath("$.elements[1].type")
                                .value("EVENT")
                )
                .andExpect(
                        jsonPath("$.elements[1].value")
                                .value("시험")
                );


        verify(
                dreamService
        )
                .getAnalysis(
                        userId,
                        dreamId
                );
    }
}