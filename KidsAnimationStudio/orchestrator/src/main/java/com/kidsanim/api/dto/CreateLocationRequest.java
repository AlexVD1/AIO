package com.kidsanim.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLocationRequest(
        @NotBlank(message = "El nombre de la locación es obligatorio")
        String name,

        @NotBlank(message = "El prompt de la locación es obligatorio")
        String prompt,

        String referenceImagePath
) {
}
