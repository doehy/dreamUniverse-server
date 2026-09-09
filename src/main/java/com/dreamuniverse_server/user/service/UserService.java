package com.dreamuniverse_server.user.service;

import com.dreamuniverse_server.user.dto.UserProfile;
import com.dreamuniverse_server.user.entity.*;
import com.dreamuniverse_server.user.repository.UserRepository;
import java.time.*;
import java.util.Optional;
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

    @Transactional(readOnly = true)
    public Optional<Long> findRegisteredUserId(OAuthProvider provider, String providerUserId) {
        return userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .filter(User::isSignupCompleted).map(User::getId);
    }

    public Long completeSignup(OAuthProvider provider, String providerUserId, String email, String name,
                               String nickname, LocalDate birthDate, Boolean termsAgreed, Boolean privacyAgreed) {
        if (nickname == null || nickname.isBlank() || nickname.length() > 30
                || birthDate == null || birthDate.isAfter(LocalDate.now())
                || !Boolean.TRUE.equals(termsAgreed) || !Boolean.TRUE.equals(privacyAgreed)
                || email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required signup information is missing or invalid");
        }
        var user = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> User.create(provider, providerUserId));
        if (user.isSignupCompleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Signup already completed; log in again");
        }
        user.completeSignup(email, name, nickname.strip(), birthDate, Instant.now());
        return userRepository.save(user).getId();
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long userId) {
        var user = userRepository.findById(userId).filter(User::isSignupCompleted)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Registered user not found"));
        return new UserProfile(user.getId(), user.getEmail(), user.getName(), user.getNickname(), user.getBirthDate());
    }
}
