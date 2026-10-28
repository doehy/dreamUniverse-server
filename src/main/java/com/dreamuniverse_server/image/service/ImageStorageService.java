package com.dreamuniverse_server.image.service;

import com.dreamuniverse_server.global.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final String CONTENT_TYPE = "image/png";

    private static final Duration PRESIGNED_GET_DURATION =
            Duration.ofMinutes(30);

    private final S3Client r2S3Client;
    private final S3Presigner r2S3Presigner;
    private final R2Properties r2Properties;

    public String upload(
            byte[] imageBytes,
            String imageKey
    ) {
        validateImageBytes(imageBytes);
        validateImageKey(imageKey);

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(
                                r2Properties.bucketName()
                        )
                        .key(imageKey)
                        .contentType(CONTENT_TYPE)
                        .contentLength(
                                (long) imageBytes.length
                        )
                        .build();

        try {
            r2S3Client.putObject(
                    request,
                    RequestBody.fromBytes(imageBytes)
            );

            return imageKey;

        } catch (S3Exception e) {
            throw new IllegalStateException(
                    "Failed to upload image to R2",
                    e
            );
        }
    }

    public String generatePresignedGetUrl(
            String imageKey
    ) {
        validateImageKey(imageKey);

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(
                                r2Properties.bucketName()
                        )
                        .key(imageKey)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(
                                PRESIGNED_GET_DURATION
                        )
                        .getObjectRequest(
                                getObjectRequest
                        )
                        .build();

        PresignedGetObjectRequest presignedRequest =
                r2S3Presigner.presignGetObject(
                        presignRequest
                );

        return presignedRequest
                .url()
                .toString();
    }

    public void delete(
            String imageKey
    ) {
        if (imageKey == null || imageKey.isBlank()) {
            return;
        }

        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(
                                r2Properties.bucketName()
                        )
                        .key(imageKey)
                        .build();

        try {
            r2S3Client.deleteObject(request);

        } catch (S3Exception e) {
            throw new IllegalStateException(
                    "Failed to delete image from R2",
                    e
            );
        }
    }

    /** Also removes old regeneration objects still under this owned dream's prefix. */
    public void deleteDreamPrefix(Long dreamId) {
        var request = ListObjectsV2Request.builder()
                .bucket(r2Properties.bucketName())
                .prefix("dreams/" + dreamId + "/")
                .build();

        for (var page : r2S3Client.listObjectsV2Paginator(request)) {
            for (var object : page.contents()) {
                delete(object.key());
            }
        }
    }

    private void validateImageBytes(
            byte[] imageBytes
    ) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException(
                    "Image data must not be empty"
            );
        }
    }

    private void validateImageKey(
            String imageKey
    ) {
        if (imageKey == null || imageKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Image key must not be blank"
            );
        }

        if (!imageKey.endsWith(".png")) {
            throw new IllegalArgumentException(
                    "Image key must end with .png"
            );
        }
    }
}