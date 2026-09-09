package com.dreamuniverse_server.user.service;

import com.dreamuniverse_server.user.dto.UserProfile;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public Long registerOAuthUser(OAuthProvider provider, String providerUserId, String email, String name) {
        userRepository.upsertOAuthUser(provider.name(), providerUserId, email, name);
        return userRepository.findByProviderAndProviderUserId(provider, providerUserId).orElseThrow().getId();
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        return new UserProfile(user.getId(), user.getEmail(), user.getName());
    }
}
