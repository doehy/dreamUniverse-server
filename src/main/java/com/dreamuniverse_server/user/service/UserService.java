package com.dreamuniverse_server.user.service;

import com.dreamuniverse_server.global.exception.CustomException;
import com.dreamuniverse_server.global.exception.ErrorCode;
import com.dreamuniverse_server.user.dto.UserProfile;
import com.dreamuniverse_server.user.entity.OAuthProvider;
import com.dreamuniverse_server.user.entity.User;
import com.dreamuniverse_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public Optional<Long> findRegisteredUserId(
            OAuthProvider provider,
            String providerUserId
    ) {
        return userRepository
                .findByProviderAndProviderUserId(
                        provider,
                        providerUserId
                )
                .filter(User::isSignupCompleted)
                .map(User::getId);
    }


    public Long completeSignup(
            OAuthProvider provider,
            String providerUserId,
            String email,
            String name,
            String nickname,
            LocalDate birthDate,
            Boolean termsAgreed,
            Boolean privacyAgreed,
            String timezone
    ) {
        /*
         * timezone은 실제 사용 가능한
         * IANA Time Zone인지 검증만 한다.
         */
        validateTimezone(timezone);

        /*
         * birthDate는 기존처럼 서버의 현재 날짜와 비교.
         *
         * AI 하루 사용량처럼 날짜 경계가 중요한
         * 비즈니스 정책이 아니므로 사용자 timezone까지
         * 적용할 필요는 없다.
         */
        if (nickname == null
                || nickname.isBlank()
                || nickname.length() > 30
                || birthDate == null
                || birthDate.isAfter(LocalDate.now())
                || !Boolean.TRUE.equals(termsAgreed)
                || !Boolean.TRUE.equals(privacyAgreed)
                || email == null
                || email.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Required signup information is missing or invalid"
            );
        }

        var user =
                userRepository
                        .findByProviderAndProviderUserId(
                                provider,
                                providerUserId
                        )
                        .orElseGet(() ->
                                User.create(
                                        provider,
                                        providerUserId
                                )
                        );

        if (user.isSignupCompleted()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Signup already completed; log in again"
            );
        }

        user.completeSignup(
                email,
                name,
                nickname.strip(),
                birthDate,
                timezone,
                Instant.now()
        );

        return userRepository
                .save(user)
                .getId();
    }


    @Transactional(readOnly = true)
    public UserProfile getProfile(Long userId) {

        var user =
                userRepository
                        .findById(userId)
                        .filter(User::isSignupCompleted)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Registered user not found"
                                )
                        );

        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getBirthDate()
        );
    }


    public void updateTimezone(
            Long userId,
            String timezone
    ) {
        validateTimezone(timezone);

        User user =
                userRepository
                        .findById(userId)
                        .filter(User::isSignupCompleted)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        user.updateTimezone(timezone);
    }


    private void validateTimezone(String timezone) {

        if (timezone == null || timezone.isBlank()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }

        try {
            ZoneId.of(timezone);

        } catch (DateTimeException e) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }
    }
}