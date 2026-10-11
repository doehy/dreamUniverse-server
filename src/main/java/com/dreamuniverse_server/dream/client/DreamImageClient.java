package com.dreamuniverse_server.dream.client;

import com.dreamuniverse_server.dream.dto.ai.DreamImageResult;
import com.openai.client.OpenAIClient;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class DreamImageClient {

    private final OpenAIClient openAIClient;

    @Value("${openai.image-model}")
    private String imageModel;


    public DreamImageResult generate(
            String prompt,
            int panelCount
    ) {
        ImageGenerateParams params =
                ImageGenerateParams.builder()
                        .model(imageModel)
                        .prompt(prompt)
                        .n(1)
                        .size(
                                getImageSize(
                                        panelCount
                                )
                        )
                        .quality(
                                getImageQuality(
                                        panelCount
                                )
                        )
                        .outputFormat(
                                ImageGenerateParams.OutputFormat.PNG
                        )
                        .build();


        ImagesResponse response =
                openAIClient
                        .images()
                        .generate(params);


        String base64Image =
                response.data()
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Image response data is empty"
                                )
                        )
                        .stream()
                        .findFirst()
                        .flatMap(
                                image -> image.b64Json()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Generated image data is missing"
                                )
                        );


        byte[] imageBytes =
                Base64.getDecoder()
                        .decode(
                                base64Image
                        );


        return new DreamImageResult(
                imageBytes
        );
    }


    private ImageGenerateParams.Size getImageSize(
            int panelCount
    ) {
        return switch (panelCount) {

            case 4 ->
                    ImageGenerateParams.Size._1024X1024;

            case 8 ->
                    ImageGenerateParams.Size.of(
                            "1024x2048"
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported panel count: "
                                    + panelCount
                    );
        };
    }


    private ImageGenerateParams.Quality getImageQuality(int panelCount) {
        return switch (panelCount) {
            case 4, 8 -> ImageGenerateParams.Quality.LOW;
            default -> throw new IllegalArgumentException(
                    "Unsupported panel count: " + panelCount
            );
        };
    }
}