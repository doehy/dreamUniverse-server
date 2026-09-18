package com.dreamuniverse_server.dream.controller;

import com.dreamuniverse_server.dream.dto.response.DreamAiUsageResponse;
import com.dreamuniverse_server.dream.service.DreamService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
}