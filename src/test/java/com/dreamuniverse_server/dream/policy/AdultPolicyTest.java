package com.dreamuniverse_server.dream.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdultPolicyTest {

    private final AdultPolicy adultPolicy =
            new AdultPolicy();


    @Test
    void 만_19세_생일_전날은_미성년이다() {

        // given
        LocalDate birthDate =
                LocalDate.of(
                        2007,
                        9,
                        18
                );

        LocalDate today =
                LocalDate.of(
                        2026,
                        9,
                        17
                );


        // when
        boolean adult =
                adultPolicy.isAdult(
                        birthDate,
                        today
                );


        // then
        assertThat(adult)
                .isFalse();
    }


    @Test
    void 만_19세_생일부터_성인이다() {

        // given
        LocalDate birthDate =
                LocalDate.of(
                        2007,
                        9,
                        18
                );

        LocalDate today =
                LocalDate.of(
                        2026,
                        9,
                        18
                );


        // when
        boolean adult =
                adultPolicy.isAdult(
                        birthDate,
                        today
                );


        // then
        assertThat(adult)
                .isTrue();
    }


    @Test
    void 만_19세를_지난_사용자는_성인이다() {

        // given
        LocalDate birthDate =
                LocalDate.of(
                        2000,
                        1,
                        1
                );

        LocalDate today =
                LocalDate.of(
                        2026,
                        9,
                        18
                );


        // when
        boolean adult =
                adultPolicy.isAdult(
                        birthDate,
                        today
                );


        // then
        assertThat(adult)
                .isTrue();
    }
}