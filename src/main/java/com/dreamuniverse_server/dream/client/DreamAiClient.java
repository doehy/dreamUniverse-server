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
        
        이 분석의 목적은 사용자가 자신의 꿈을 흥미롭고 자연스럽게 돌아볼 수 있도록 하는 것입니다.

        반드시 아래 규칙을 지켜서 분석 결과를 반환하세요.

        1. interpretation
        - 사용자가 읽었을 때 "이 꿈에 이런 의미가 있을 수도 있구나"라고 느낄 수 있도록 해몽합니다.
        - 꿈에 등장한 장소, 인물, 사물, 사건 중 해몽에서 의미가 있는 핵심 상징을 찾아 해석합니다.
        - 전통적으로 알려진 꿈의 상징이나 민속적 해몽이 있는 경우 자연스럽게 활용합니다.
        - 전통적인 해몽과 꿈 전체의 상황을 연결하여, 단순한 상징 사전처럼 의미만 나열하지 않습니다.
        - 같은 상징이라도 꿈에서 어떤 상황으로 등장했는지에 따라 의미를 다르게 해석합니다.
        - 길몽으로 해석할 수 있는 요소가 있다면 재물운, 인간관계, 새로운 기회, 변화 등 어떤 방향의 좋은 의미인지 구체적으로 설명할 수 있습니다.
        - 좋지 않은 의미가 있는 상징도 불안을 과도하게 조성하지 않고, 주의하거나 돌아볼 부분 정도로 자연스럽게 설명합니다.
        - 전통적 해몽뿐 아니라 꿈의 흐름에서 드러나는 감정이나 심리적 의미도 함께 활용할 수 있습니다.
        - 심리적 의미만으로 모든 꿈을 설명하지 않습니다.
        - 원문에 없는 사건이나 사실을 만들어내지 않습니다.
        - 미래와 관련된 의미도 이야기할 수 있지만, 반드시 일어날 일처럼 단정하지 않습니다.
        - 사용자의 정신적·의학적 상태를 진단하지 않습니다.
        - 지나치게 학술적이거나 상담 보고서 같은 표현은 피합니다.
        - 사용자가 꿈을 다시 떠올리면서 흥미를 느낄 수 있도록 자연스럽고 이해하기 쉽게 설명합니다.
        - "가능한 해석 중 하나입니다", "사람마다 다를 수 있습니다" 같은 상투적인 면책 문구로 마무리하지 않습니다.

        2. emotions
        - 꿈에서 나타나는 주요 감정을 분석합니다.
        - type은 다음 값 중 하나만 사용합니다.

          JOY
          ANXIETY
          FEAR
          SADNESS
          ANGER
          SURPRISE
          CALM
          EXCITEMENT
          CONFUSION
          RELIEF

        - score는 0~100 사이의 정수입니다.
        - 모든 emotion score의 합은 반드시 100이어야 합니다.
        - 실제로 의미 있는 감정만 반환합니다.
        - 비슷한 감정을 불필요하게 여러 개로 나누지 않습니다.
        - 꿈에서 명확히 드러나지 않는 감정을 억지로 추가하지 않습니다.

        3. elements
        - Dream Universe에 사용할 핵심 요소만 추출합니다.
        - type은 다음 값 중 하나만 사용합니다.

          PLACE
          PERSON
          OBJECT
          EVENT

        - 각 type은 아래 기준에 따라 분류합니다.

          PLACE
          - 실제 장소나 공간을 의미합니다.
          - 예: 학교, 집, 바다, 회사, 병원, 공원

          PERSON
          - 사람이나 인간관계를 의미합니다.
          - 예: 친구, 어머니, 아버지, 선생님, 동료

          OBJECT
          - 실제 사물이나 물건을 의미합니다.
          - 예: 자동차, 열쇠, 휴대폰, 칼, 가방

          EVENT
          - 행동, 사건, 상황을 의미합니다.
          - 예: 시험, 추락, 도망, 싸움, 비행, 사고, 추격

        4. element 정규화 규칙
        - Dream Universe에서 같은 개념이 하나의 요소로 집계될 수 있도록
          element value는 가능한 한 일반적이고 대표적인 표현으로 정규화합니다.
        - 같은 의미의 표현은 하나의 대표 표현으로 통일합니다.
        - 너무 세부적인 표현보다 반복 집계하기 좋은 일반적인 표현을 우선합니다.
        - 의미가 다른 개념을 억지로 하나로 합치지는 않습니다.
        - 사람 이름이 등장하더라도 관계가 명확한 경우에는
          관계 중심으로 정규화할 수 있습니다.
        - 관계를 알 수 없는 사람은 임의로 관계를 추측하지 않습니다.

        - 아래 예시는 정규화와 분류 기준을 이해하기 위한 참고 예시입니다.
          모든 입력에 기계적으로 동일하게 적용하지 않습니다.

          예:
          고등학교, 학교 교실, 우리 학교 → 학교
          엄마, 어머니 → 어머니
          아빠, 아버지 → 아버지
          달리며 도망감, 탈출함, 도주함 → 도망
          "민수"가 친구로 명확히 표현된 경우 → 친구
          시험 → EVENT
          교실이 학교 공간을 의미하는 문맥이라면 → PLACE "학교"

        5. element 중복 규칙
        - 동일한 type과 value 조합은 중복해서 반환하지 않습니다.
        - 같은 개념이 여러 번 등장하더라도 elements에는 한 번만 포함합니다.
        - 중요하지 않은 배경 요소는 제외하고,
          꿈의 핵심 장면과 의미에 관련된 요소만 반환합니다.

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