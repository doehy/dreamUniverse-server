package com.dreamuniverse_server.user.controller;

import com.dreamuniverse_server.user.dto.request.UserTimezoneRequest;
import com.dreamuniverse_server.user.service.AccountDeletionService;
import com.dreamuniverse_server.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Jwt jwt) {
        accountDeletionService.delete(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }



    @PatchMapping("/me/timezone")
    public ResponseEntity<Void> updateTimezone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserTimezoneRequest request
    ) {
        Long userId =
                Long.valueOf(jwt.getSubject());

        userService.updateTimezone(
                userId,
                request.timezone()
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}