package com.dreamuniverse_server.dream.service;

import com.dreamuniverse_server.global.config.R2Properties;
import com.dreamuniverse_server.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
class DreamImageServiceIntegrationTest {

    @Autowired
    private DreamImageService dreamImageService;

    @Autowired
    private S3Client r2S3Client;

    @Autowired
    private R2Properties r2Properties;


    @Test
    void 실제_OpenAI에서_이미지를_생성하고_R2에_업로드한다() {

        // given
        Long dreamId = 999999L;

        String content = """
                밤하늘을 날아다니고 있었다.
                아래에는 작은 도시의 불빛들이 보였다.
                계속 날아가다가 눈앞에 아주 거대한 보름달이 나타났다.
                나는 놀라서 달을 바라보았다.
                """;

        User user = mock(User.class);

        given(user.getBirthDate())
                .willReturn(
                        LocalDate.of(
                                2000,
                                1,
                                1
                        )
                );

        given(user.getTimezone())
                .willReturn(
                        "Asia/Seoul"
                );


        String imageKey = null;

        try {

            // when
            imageKey =
                    dreamImageService.generateAndUpload(
                            user,
                            dreamId,
                            content
                    );


            // then
            assertThat(imageKey)
                    .isNotBlank()
                    .startsWith(
                            "dreams/"
                                    + dreamId
                                    + "/"
                    )
                    .endsWith(
                            ".png"
                    );


            /*
             * R2에 실제 객체가 존재하는지 확인
             */
            HeadObjectResponse head =
                    r2S3Client.headObject(
                            HeadObjectRequest.builder()
                                    .bucket(
                                            r2Properties.bucketName()
                                    )
                                    .key(
                                            imageKey
                                    )
                                    .build()
                    );


            assertThat(
                    head.contentLength()
            ).isGreaterThan(0);


            assertThat(
                    head.contentType()
            ).isEqualTo(
                    "image/png"
            );


            /*
             * R2에 저장된 실제 파일을 다시 다운로드해서
             * PNG 파일인지 확인한다.
             */
            ResponseBytes<GetObjectResponse> objectBytes =
                    r2S3Client.getObjectAsBytes(
                            GetObjectRequest.builder()
                                    .bucket(
                                            r2Properties.bucketName()
                                    )
                                    .key(
                                            imageKey
                                    )
                                    .build()
                    );


            byte[] downloadedImage =
                    objectBytes.asByteArray();


            assertThat(downloadedImage)
                    .isNotEmpty();


            /*
             * PNG signature
             *
             * 89 50 4E 47 0D 0A 1A 0A
             */
            assertThat(downloadedImage)
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


            System.out.println(
                    "R2 image key: "
                            + imageKey
            );

            System.out.println(
                    "R2 image size: "
                            + downloadedImage.length
                            + " bytes"
            );

        } finally {
            if (imageKey != null) {
                try {
                    r2S3Client.deleteObject(
                            DeleteObjectRequest.builder()
                                    .bucket(
                                            r2Properties.bucketName()
                                    )
                                    .key(
                                            imageKey
                                    )
                                    .build()
                    );

                } catch (RuntimeException e) {
                    System.err.println(
                            "R2 test image cleanup failed: "
                                    + imageKey
                    );
                }
            }
        }
    }
}