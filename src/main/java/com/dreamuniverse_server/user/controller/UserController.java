package com.dreamuniverse_server.user.controller;

import com.dreamuniverse_server.user.dto.request.UserTimezoneRequest;
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