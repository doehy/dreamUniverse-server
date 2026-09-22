package com.dreamuniverse_server.image.service;

import com.dreamuniverse_server.global.config.R2Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "R2_ACCOUNT_ID",
        matches = ".+"
)
class ImageStorageServiceIntegrationTest {

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private S3Client r2S3Client;

    @Autowired
    private R2Properties r2Properties;


    @Test
    void png_이미지를_R2에_업로드하고_삭제한다() {

        // given
        byte[] imageBytes =
                Base64.getDecoder()
                        .decode(
                                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB"
                                        + "CAQAAAC1HAwCAAAAC0lEQVR42mNk"
                                        + "YAAAAAYAAjCB0C8AAAAASUVORK5CYII="
                        );

        String imageKey =
                "test/%s.png"
                        .formatted(
                                UUID.randomUUID()
                        );

        try {

            // when
            String uploadedKey =
                    imageStorageService.upload(
                            imageBytes,
                            imageKey
                    );


            // then
            assertThat(uploadedKey)
                    .isEqualTo(imageKey);


            var headResponse =
                    r2S3Client.headObject(
                            HeadObjectRequest.builder()
                                    .bucket(
                                            r2Properties.bucketName()
                                    )
                                    .key(imageKey)
                                    .build()
                    );


            assertThat(
                    headResponse.contentLength()
            ).isEqualTo(
                    (long) imageBytes.length
            );

            assertThat(
                    headResponse.contentType()
            ).isEqualTo(
                    "image/png"
            );

        } finally {

            imageStorageService.delete(
                    imageKey
            );
        }
    }
}