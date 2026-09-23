package com.dreamuniverse_server.dream.client;

import com.dreamuniverse_server.dream.dto.ai.DreamImageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIf(
        expression = "#{environment['OPENAI_API_KEY'] != null}",
        reason = "OPENAI_API_KEY가 있을 때만 실제 OpenAI 이미지 API를 호출합니다."
)
class DreamImageClientIntegrationTest {

    @Autowired
    private DreamImageClient dreamImageClient;


    @Test
    void 실제_OpenAI_이미지_API를_호출해서_PNG이미지를_생성한다() throws Exception {

        // given
        String prompt = """
                꿈을 바탕으로 컬러 웹툰 스타일의 4컷 만화를 생성한다.

                정확히 4개의 패널로 구성한다.
                패널은 2x2 배열이다.
                읽는 순서는 왼쪽 위, 오른쪽 위, 왼쪽 아래, 오른쪽 아래이다.

                한 사람이 밤하늘을 날아다니다가
                거대한 달을 발견하는 꿈이다.

                동일 인물의 외형을 모든 패널에서 일관되게 유지한다.
                """;


        // when
        DreamImageResult result =
                dreamImageClient.generate(
                        prompt
                );


        // then
        assertThat(result)
                .isNotNull();

        assertThat(result.imageBytes())
                .isNotNull()
                .isNotEmpty();


        /*
         * PNG 파일 시그니처 확인
         *
         * PNG 파일은 항상 앞부분이
         *
         * 89 50 4E 47 0D 0A 1A 0A
         *
         * 로 시작한다.
         */
        byte[] imageBytes =
                result.imageBytes();

        assertThat(imageBytes)
                .startsWith(
                        (byte) 0x89,
                        (byte) 0x50,
                        (byte) 0x4E,
                        (byte) 0x47,
                        (byte) 0x0D,
                        (byte) 0x0A,
                        (byte) 0x1A,
                        (byte) 0x0A
                );


        /*
         * 사람이 직접 결과를 확인할 수 있도록
         * 테스트 결과 이미지를 임시 파일로 저장한다.
         */
        Path outputPath =
                Path.of(
                        "build",
                        "test-output",
                        "dream-image-test.png"
                );

        Files.createDirectories(
                outputPath.getParent()
        );

        Files.write(
                outputPath,
                imageBytes
        );

        System.out.println(
                "Generated image: "
                        + outputPath.toAbsolutePath()
        );
    }
}