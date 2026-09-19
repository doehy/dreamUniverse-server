package com.dreamuniverse_server.dream.policy;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AdultPolicy {

    private static final int ADULT_AGE = 19;

    public boolean isAdult(
            LocalDate birthDate,
            LocalDate today
    ) {
        if (birthDate == null || today == null) {
            return false;
        }

        return !birthDate
                .plusYears(ADULT_AGE)
                .isAfter(today);
    }
}