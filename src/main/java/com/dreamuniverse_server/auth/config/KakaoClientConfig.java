package com.dreamuniverse_server.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoClientConfig {
    @Bean("kakaoRestClient")
    RestClient kakaoRestClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        return RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .requestFactory(requestFactory)
                .build();
    }
}
