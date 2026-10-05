package com.kidsanim.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSeriesRequest(
        @NotBlank(message = "El nombre de la serie es obligatorio")
        String name,

        String description,

        String language,

        @Min(value = 0, message = "La edad mínima debe ser mayor o igual a 0")
        Integer targetAgeMin,

        @Min(value = 1, message = "La edad máxima debe ser al menos 1")
        Integer targetAgeMax,

        String aspectRatio,

        @NotNull(message = "El styleProfileId es obligatorio")
        UUID styleProfileId,

        String defaultBgmMood
) {
    public CreateSeriesRequest {
        if (language == null || language.isBlank()) language = "es-MX";
        if (targetAgeMin == null) targetAgeMin = 2;
        if (targetAgeMax == null) targetAgeMax = 6;
        if (aspectRatio == null || aspectRatio.isBlank()) aspectRatio = "16:9";
        if (defaultBgmMood == null || defaultBgmMood.isBlank()) defaultBgmMood = "playful";
    }
}
