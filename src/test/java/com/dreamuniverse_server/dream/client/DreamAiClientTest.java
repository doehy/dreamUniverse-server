package com.dreamuniverse_server.dream.client;

import com.dreamuniverse_server.dream.dto.ai.DreamAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DreamAiClientTest {

    @Autowired
    private DreamAiClient dreamAiClient;

    @Test
    void 꿈_분석_테스트() {
        String content = """
                밤에 오래된 집에 혼자 있었는데, 갑자기 벽에 있던 문 하나가 저절로 열렸어.
               문 밖에는 처음 보는 넓은 정원이 있었고, 정원 한가운데 큰 나무에 금빛 열매가 주렁주렁 열려 있었어.
               열매 하나를 따려고 다가가는데 돌아가신 할머니가 나타나서 웃으면서 그냥 가져가라고 했어.
               그래서 열매 하나를 따서 주머니에 넣었는데, 그 순간 집 안에서 큰 종소리가 들리고 잠에서 깼어.
               """;

        DreamAnalysisResult result =
                dreamAiClient.analyze(content);

        System.out.println("interpretation = " + result.getInterpretation());

        result.getEmotions().forEach(emotion ->
                System.out.println(
                        emotion.getType() + " : " + emotion.getScore()
                )
        );

        result.getElements().forEach(element ->
                System.out.println(
                        element.getType() + " : " + element.getValue()
                )
        );
    }
}