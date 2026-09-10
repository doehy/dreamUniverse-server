package com.dreamuniverse_server.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenInfo(Long id, @JsonProperty("app_id") Long appId,
                             @JsonProperty("expires_in") Long expiresIn) {}
