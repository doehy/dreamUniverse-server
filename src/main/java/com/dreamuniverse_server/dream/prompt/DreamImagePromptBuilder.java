package com.dreamuniverse_server.dream.prompt;

import org.springframework.stereotype.Component;

@Component
public class DreamImagePromptBuilder {

    private static final String COMMON_RULES = """
            [공통 절대 규칙]
            - 꿈의 핵심 사건, 분위기, 감정은 가능한 한 유지한다.
            - 미성년자로 명시되거나 미성년임이 명확한 인물을 성적으로 묘사하지 않는다.
            - 성적인 맥락의 인물에게 미성년이라는 단서가 없다면,
              이미지에서는 해당 인물을 명백한 성인으로 표현한다.
            - 노골적인 성행위, 성기 노출, 과도한 노출은 표현하지 않는다.
            - 성폭력, 강압적 성적 상황, 아동 대상 성적 표현은 생성하지 않는다.
            - 심한 고어, 신체 절단, 내장 노출, 극단적인 신체 훼손은 표현하지 않는다.
            - 자해, 자살, 학대 장면은 자극적이거나 미화된 방식으로 묘사하지 않는다.
            - 금지되는 장면이 포함된 경우 꿈의 의미와 흐름을 유지하면서
              안전하고 상징적인 장면으로 자연스럽게 순화한다.
            """;

    private static final String MINOR_RULES = """
            [미성년 사용자 추가 규칙]
            - 폭력적이거나 공포스러운 장면은 직접적인 피, 상처, 훼손을 최소화한다.
            - 성적인 상황은 로맨틱하거나 상징적인 분위기로만 표현한다.
            - 성행위나 노출을 직접적으로 표현하지 않는다.
            - 꿈의 긴장감과 감정은 유지하되 시각적 자극은 낮춘다.
            - 전체적으로 비노골적이고 안전한 표현을 사용한다.
            """;

    private static final String ADULT_RULES = """
            [성인 사용자 추가 규칙]
            - 사용자가 기록한 꿈의 사건과 분위기를 가능한 한 충실하게 표현한다.
            - 폭력적이거나 관능적인 분위기가 있어도 꿈의 맥락은 유지한다.
            - 다만 공통 절대 규칙을 항상 우선한다.
            - 심한 고어, 노골적인 성행위, 과도한 노출은 표현하지 않는다.
            - 필요한 경우 핵심 의미를 유지한 채 자연스럽게 순화한다.
            """;

    private static final String SINGLE_IMAGE_RULES = """
            [1장만 생성할 때의 스토리 규칙]
            - 꿈 전체를 억지로 4컷 안에 압축하거나 요약하지 않는다.
            - 꿈의 시작 부분부터 실제 사건이 진행된 순서대로 장면을 선택한다.
            - 4컷 안에 자연스럽게 담을 수 있는 범위까지만 표현한다.
            - 아직 꿈의 뒷내용이 남아 있어도 결말을 임의로 앞당겨 표현하지 않는다.
            - 꿈의 후반부 사건을 앞부분과 섞어서 보여주지 않는다.
            - 마지막 컷이 이야기 중간에서 끝나는 느낌이어도 괜찮다.
            - 사용자가 이어지는 다음 장면을 자연스럽게 궁금해할 수 있도록
              원래 꿈의 흐름을 그대로 유지한다.
            """;

    public String build(
            String dreamContent,
            boolean adult,
            int maxImageCount
    ) {
        validateMaxImageCount(maxImageCount);

        String ageRules =
                adult
                        ? ADULT_RULES
                        : MINOR_RULES;

        String imageCountRules =
                maxImageCount == 1
                        ? SINGLE_IMAGE_RULES
                        : buildMultiImageRules(maxImageCount);

        return """
                당신은 사용자가 실제로 기록한 꿈을
                감성적인 4컷 꿈 만화 이미지로 표현합니다.

                %s

                %s

                %s

                [이미지 레이아웃 규칙]
                - 이번 요청에서는 이미지 한 장을 생성한다.
                - 한 장의 이미지 안에 정확히 4개의 패널을 구성한다.
                - 패널은 2x2 격자 형태로 배치한다.
                - 장면은 왼쪽 위 → 오른쪽 위 → 왼쪽 아래 → 오른쪽 아래 순서로 진행된다.
                - 네 컷의 시간적 흐름이 자연스럽게 이어져야 한다.
                - 같은 인물이 여러 컷에 등장하면 외형, 머리 모양, 의상 등의 특징을 일관되게 유지한다.
                - 각 패널의 경계를 분명하게 표현한다.
                - 각 컷의 주요 장면이 충분히 크게 보이도록 구성한다.

                [그림 표현 규칙]
                - 실제 사진보다는 감성적인 디지털 일러스트 스타일로 표현한다.
                - 꿈 특유의 몽환적이고 비현실적인 분위기를 살린다.
                - 이미지 안에 글자, 자막, 설명문, 말풍선, 로고를 넣지 않는다.
                - 사용자의 꿈에 없는 새로운 핵심 사건을 임의로 추가하지 않는다.
                - 인물의 외형이 명시되지 않았다면 꿈의 문맥에 맞게 자연스럽게 보완한다.
                - 안전 규칙과 꿈 내용이 충돌하면 안전 규칙을 우선한다.

                [사용자의 꿈 원문]
                ---
                %s
                ---
                """.formatted(
                COMMON_RULES,
                ageRules,
                imageCountRules,
                dreamContent
        );
    }

    private String buildMultiImageRules(int maxImageCount) {

        return """
                [여러 장을 사용할 수 있을 때의 스토리 규칙]
                - 이 사용자는 최대 %d장의 이미지를 사용할 수 있다.
                - 꿈 전체를 한 장에 억지로 압축하지 않는다.
                - 꿈의 사건을 처음부터 끝까지 시간 순서대로 여러 이미지에 분배할 수 있다.
                - 각 이미지 역시 앞 이미지의 다음 장면부터 자연스럽게 이어져야 한다.
                - 후반부 사건을 앞 이미지에 미리 보여주지 않는다.
                - 꿈의 분량이 짧다면 허용된 최대 장수를 반드시 모두 사용할 필요는 없다.
                """.formatted(maxImageCount);
    }

    private void validateMaxImageCount(int maxImageCount) {

        if (maxImageCount < 1 || maxImageCount > 3) {
            throw new IllegalArgumentException(
                    "maxImageCount must be between 1 and 3: "
                            + maxImageCount
            );
        }
    }
}