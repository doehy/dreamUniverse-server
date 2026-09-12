package com.dreamuniverse_server.dream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DreamCreateRequest(

        @NotBlank
        @Size(max = 1000)
        String content,

        @NotNull
        @PastOrPresent
        LocalDate dreamDate,

        boolean aiRequested

) {
}