package com.dreamuniverse_server.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserTimezoneRequest(

        @NotBlank
        @Size(max = 64)
        String timezone

) {
}