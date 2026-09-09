package com.dreamuniverse_server.auth.dto.response;

import com.dreamuniverse_server.user.entity.OAuthProvider;

public record UserInfo(OAuthProvider provider, String providerUserId, String email, String name) {}
