package com.dreamuniverse_server.dream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DreamUpdateRequest(

        @NotBlank
        @Size(max = 1000)
        String content,

        boolean aiRequested

) {
}