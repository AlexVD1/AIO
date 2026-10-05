package com.kidsanim.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateSeriesRequest(
        @NotBlank(message = "El nombre de la serie es obligatorio")
        String name,

        String description,

        @NotBlank(message = "El idioma es obligatorio")
        String language,

        @Min(value = 0, message = "La edad mínima debe ser mayor o igual a 0")
        Integer targetAgeMin,

        @Min(value = 1, message = "La edad máxima debe ser al menos 1")
        Integer targetAgeMax,

        @NotBlank(message = "El aspect ratio es obligatorio")
        String aspectRatio,

        @NotNull(message = "El styleProfileId es obligatorio")
        UUID styleProfileId,

        @NotBlank(message = "El mood musical es obligatorio")
        String defaultBgmMood
) {
}
