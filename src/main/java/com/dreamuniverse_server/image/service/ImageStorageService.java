package com.dreamuniverse_server.image.service;

import com.dreamuniverse_server.global.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final String CONTENT_TYPE = "image/png";

    private final S3Client r2S3Client;
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