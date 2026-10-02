package com.storyvideo.api.batch.dto;

import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryTone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record BatchGenerationRequestDto(
        @NotEmpty(message = "La distribución por género es obligatoria")
        Map<StoryGenre, Integer> distribution,

        StoryTone tone,
        String language,

        @Min(value = 30, message = "La duración mínima es de 30 segundos")
        @Max(value = 180, message = "La duración máxima es de 180 segundos")
        Integer targetDurationSeconds,

        @Min(value = 3, message = "El número mínimo de escenas es 3")
        @Max(value = 10, message = "El número máximo de escenas es 10")
        Integer sceneCount,

        String visualStyleName,
        String ttsVoice,

        @Min(value = 1, message = "La concurrencia mínima es 1")
        @Max(value = 5, message = "La concurrencia máxima es 5 para evitar sobrecarga de GPU/VRAM")
        Integer maxConcurrency
) {
    public int resolvedMaxConcurrency() {
        return (maxConcurrency != null && maxConcurrency >= 1) ? maxConcurrency : 1;
    }

    public String resolvedLanguage() {
        return (language != null && !language.isBlank()) ? language : "es-MX";
    }

    public int resolvedTargetDurationSeconds() {
        return (targetDurationSeconds != null && targetDurationSeconds >= 30) ? targetDurationSeconds : 90;
    }

    public int resolvedSceneCount() {
        return (sceneCount != null && sceneCount >= 3) ? sceneCount : 5;
    }

    public StoryTone resolvedTone() {
        return tone != null ? tone : StoryTone.DARK;
    }

    public int totalStories() {
        if (distribution == null) return 0;
        return distribution.values().stream().filter(v -> v != null && v > 0).mapToInt(Integer::intValue).sum();
    }
}
