package com.kidsanim.api.dto;

import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.CharacterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateCharacterRequest(
        @NotBlank(message = "El nombre del personaje es obligatorio")
        String name,

        @NotNull(message = "El rol es obligatorio")
        CharacterRole role,

        @NotBlank(message = "El prompt canónico es obligatorio")
        String canonicalPrompt,

        String personality,

        @NotBlank(message = "La voz TTS es obligatoria")
        String ttsVoice,

        @NotBlank(message = "El rate TTS es obligatorio")
        String ttsRate,

        @NotBlank(message = "El pitch TTS es obligatorio")
        String ttsPitch,

        String referenceImagePath,

        Long referenceSeed,

        @NotNull(message = "El peso IP-Adapter es obligatorio")
        BigDecimal ipadapterWeight,

        @NotNull(message = "El estado es obligatorio")
        CharacterStatus status
) {
}
