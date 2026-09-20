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

    @Value("${openai.image-quality}")
    private String imageQuality;


    public DreamImageResult generate(String prompt) {

        ImageGenerateParams params =
                ImageGenerateParams.builder()
                        .model(imageModel)
                        .prompt(prompt)
                        .n(1)
                        .size(
                                ImageGenerateParams.Size._1024X1024
                        )
                        .quality(
                                getImageQuality()
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
                        .decode(base64Image);


        return new DreamImageResult(
                imageBytes
        );
    }


    private ImageGenerateParams.Quality getImageQuality() {

        return switch (
                imageQuality.toLowerCase()
                ) {
            case "low" ->
                    ImageGenerateParams.Quality.LOW;

            case "medium" ->
                    ImageGenerateParams.Quality.MEDIUM;

            case "high" ->
                    ImageGenerateParams.Quality.HIGH;

            case "auto" ->
                    ImageGenerateParams.Quality.AUTO;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported image quality: "
                                    + imageQuality
                    );
        };
    }
}