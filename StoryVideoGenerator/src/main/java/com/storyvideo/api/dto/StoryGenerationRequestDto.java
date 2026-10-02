package com.storyvideo.api.dto;

import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryTone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StoryGenerationRequestDto(
        @NotNull(message = "El género narrativo es obligatorio")
        StoryGenre genre,

        StoryTone tone,

        String theme,

        String subgenre,

        @Min(value = 30, message = "La duración mínima es de 30 segundos")
        @Max(value = 180, message = "La duración máxima es de 180 segundos")
        Integer targetDurationSeconds,

        String language,

        @Min(value = 3, message = "El número mínimo de escenas es 3")
        @Max(value = 10, message = "El número máximo de escenas es 10")
        Integer sceneCount,

        String visualStyleName,

        String ttsVoice
) {
    public StoryTone resolvedTone() {
        return tone != null ? tone : StoryTone.DARK;
    }

    public int resolvedTargetDurationSeconds() {
        return (targetDurationSeconds != null && targetDurationSeconds >= 30) ? targetDurationSeconds : 90;
    }

    public String resolvedLanguage() {
        return (language != null && !language.isBlank()) ? language : "es-MX";
    }

    public int resolvedSceneCount() {
        return (sceneCount != null && sceneCount >= 3) ? sceneCount : 5;
    }
}
