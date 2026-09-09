package com.dreamuniverse_server.user.dto;

import java.time.LocalDate;

public record UserProfile(Long userId, String email, String name, String nickname, LocalDate birthDate) {}
