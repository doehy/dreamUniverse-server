package com.dreamuniverse_server.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2Config {

    @Bean
    public S3Client r2S3Client(
            R2Properties properties
    ) {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        properties.accessKeyId(),
                        properties.secretAccessKey()
                );

        S3Configuration s3Configuration =
                S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build();

        return S3Client.builder()
                .endpointOverride(
                        properties.endpoint()
                )
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                credentials
                        )
                )
                .region(
                        Region.of("auto")
                )
                .serviceConfiguration(
                        s3Configuration
                )
                .build();
    }

    @Bean
    public S3Presigner r2S3Presigner(
            R2Properties properties
    ) {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        properties.accessKeyId(),
                        properties.secretAccessKey()
                );

        S3Configuration s3Configuration =
                S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build();

        return S3Presigner.builder()
                .endpointOverride(
                        properties.endpoint()
                )
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                credentials
                        )
                )
                .region(
                        Region.of("auto")
                )
                .serviceConfiguration(
                        s3Configuration
                )
                .build();
    }
}