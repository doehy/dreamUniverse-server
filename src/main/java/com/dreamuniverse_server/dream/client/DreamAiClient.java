package com.dreamuniverse_server.dream.client;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DreamAiClient {

    private final OpenAIClient openAIClient;

    @Value("${openai.text-model}")
    private String textModel;

    @Value("${openai.image-model}")
    private String imageModel;

    @Value("${openai.image-quality}")
    private String imageQuality;

    public DreamAnalysisResult analyze(String content) {

        String prompt = """
                아래 사용자의 꿈을 분석하세요.

                1. summary
                - 꿈의 핵심 내용을 간결하게 요약합니다.

                2. interpretation
                - 꿈의 내용을 바탕으로 심리적·상징적 의미를 설명합니다.
                - 확정적인 사실처럼 표현하지 않습니다.

                3. emotions
                - 주요 감정을 분석합니다.
                - score는 0~100의 정수입니다.
                - 모든 score의 합은 100입니다.

                4. elements
                - Dream Universe에 사용할 핵심 요소를 추출합니다.
                - type은 PLACE, PERSON, OBJECT, EVENT 중 하나입니다.
                - 같은 의미는 가능한 한 일반적인 단어로 정규화합니다.

                꿈 내용:
                """ + content;

        StructuredResponseCreateParams<DreamAnalysisResult> params =
                ResponseCreateParams.builder()
                        .model(textModel)
                        .input(prompt)
                        .text(DreamAnalysisResult.class)
                        .build();

        StructuredResponse<DreamAnalysisResult> response =
                openAIClient.responses().create(params);

        return response.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(contentItem -> contentItem.outputText().stream())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "OpenAI analysis response is empty"
                        )
                );
    }
}