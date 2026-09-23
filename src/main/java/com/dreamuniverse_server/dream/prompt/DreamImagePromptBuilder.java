package com.dreamuniverse_server.dream.prompt;

import org.springframework.stereotype.Component;

@Component
public class DreamImagePromptBuilder {

    private static final String COMMON_RULES = """
            [공통 안전 규칙]
            - 꿈의 전체적인 의미와 사건의 흐름을 가능한 한 유지한다.
            - 등장인물의 외형과 복장은 패널 사이에서 일관되게 유지한다.
            - 미성년자로 명시되었거나 미성년자로 판단되는 인물을 성적으로 묘사하지 않는다.
            - 성적인 상황에서 등장인물의 나이가 명확하지 않고 미성년자라는 단서가 없다면 성인으로 표현한다.
            - 사용자가 미성년자이고 본인이 등장하는 성적인 꿈이라면 해당 장면은 비성적인 상징적 표현으로 완화한다.
            - 노골적인 성행위, 성기 노출, 과도한 나체를 표현하지 않는다.
            - 성폭력, 강압적 성행위, 아동 성적 콘텐츠를 표현하지 않는다.
            - 심각한 고어, 신체 절단, 내장 노출, 극단적인 신체 훼손을 표현하지 않는다.
            - 자해, 자살, 학대가 등장하더라도 그래픽하거나 미화된 방식으로 표현하지 않는다.
            - 위험하거나 제한되는 장면은 꿈의 의미가 최대한 유지되도록 상징적이고 비그래픽한 장면으로 바꾼다.
            """;

    private static final String MINOR_RULES = """
            [미성년 사용자 추가 규칙]
            - 성적이거나 선정적인 장면은 직접 묘사하지 않고 상징적이고 비성적인 장면으로 바꾼다.
            - 노출이 강조되는 의상이나 자세를 사용하지 않는다.
            - 폭력적인 장면은 강도를 낮추고 비그래픽하게 표현한다.
            """;

    private static final String ADULT_RULES = """
            [성인 사용자 규칙]
            - 안전 규칙을 지키는 범위 안에서 꿈의 분위기와 내용을 가능한 한 충실하게 표현한다.
            - 공포, 불안, 로맨스, 긴장감 같은 감정적 분위기는 꿈의 원래 느낌을 유지한다.
            """;

    private static final String CARTOON_RULES = """
            [만화 표현 규칙]
            - 하나의 이미지는 하나의 4컷 만화 페이지처럼 구성한다.
            - 정확히 4개의 패널로 구성한다.
            - 패널은 2x2 배열로 배치한다.
            - 읽는 순서는 왼쪽 위 → 오른쪽 위 → 왼쪽 아래 → 오른쪽 아래이다.
            - 각 패널은 시간의 흐름에 따라 자연스럽게 이어져야 한다.
            - 동일 인물은 모든 패널에서 알아볼 수 있도록 외형을 일관되게 유지한다.
            - 장면을 설명하기 위한 임의의 자막, 설명문, 제목, 워터마크, 로고는 넣지 않는다.
            - 꿈 원문에 실제 대사가 존재하는 경우에만 말풍선을 사용할 수 있다.
            - 말풍선에는 꿈 원문에 존재하는 대사를 최대한 그대로 사용한다.
            - 원문에 없는 새로운 대사를 임의로 만들어내지 않는다.
            """;

    private static final String SINGLE_IMAGE_RULES = """
            [이미지 1장 생성 규칙]
            - 꿈 전체를 억지로 4개의 패널에 압축하지 않는다.
            - 반드시 꿈의 시작 부분부터 시간 순서대로 장면을 선택한다.
            - 4개의 패널 안에 자연스럽게 들어가는 부분까지만 표현한다.
            - 뒤쪽 사건이나 결말을 앞쪽으로 당겨 표현하지 않는다.
            - 꿈의 초반 장면과 후반 장면을 섞지 않는다.
            - 꿈이 길어서 중간이나 결말이 이미지에 등장하지 않아도 괜찮다.
            """;

    private static final String MULTI_IMAGE_RULES = """
            [여러 이미지 생성 가능 규칙]
            - 각 이미지는 하나의 4컷 만화 페이지로 생각한다.
            - 꿈의 사건을 처음부터 시간 순서대로 이어서 표현한다.
            - 앞 이미지에서 표현한 장면을 다음 이미지에서 불필요하게 반복하지 않는다.
            - 꿈의 중요한 사건 흐름이 자연스럽게 이어지도록 페이지를 나눈다.
            - 전체 이미지 수는 주어진 최대 이미지 수를 초과해서는 안 된다.
            """;


    public String build(
            String dreamContent,
            boolean adult,
            int maxImageCount
    ) {
        String ageRules =
                adult
                        ? ADULT_RULES
                        : MINOR_RULES;

        String imageCountRules =
                maxImageCount == 1
                        ? SINGLE_IMAGE_RULES
                        : MULTI_IMAGE_RULES;

        return """
                사용자가 작성한 꿈을 바탕으로 만화 스타일의 이미지를 생성한다.

                %s

                %s

                %s

                %s

                [이미지 생성 조건]
                - 생성 가능한 최대 이미지 수: %d
                - 꿈의 사건, 장소, 인물, 감정과 분위기를 시각적으로 표현한다.
                - 현실적인 사진보다는 완성도 높은 컬러 만화/웹툰 스타일로 표현한다.
                - 패널 사이의 시간 흐름이 명확하게 느껴지도록 구성한다.

                [꿈 원문]
                %s
                """.formatted(
                COMMON_RULES,
                ageRules,
                CARTOON_RULES,
                imageCountRules,
                maxImageCount,
                dreamContent
        );
    }
}