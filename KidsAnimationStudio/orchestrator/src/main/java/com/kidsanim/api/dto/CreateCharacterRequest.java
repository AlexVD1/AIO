package com.kidsanim.api.dto;

import com.kidsanim.api.domain.enums.CharacterRole;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateCharacterRequest(
        @NotBlank(message = "El nombre del personaje es obligatorio")
        String name,

        CharacterRole role,

        @NotBlank(message = "El prompt canónico es obligatorio")
        String canonicalPrompt,

        String personality,

        String ttsVoice,

        String ttsRate,

        String ttsPitch,

        String voiceEngine,

        String voiceReferencePath,

        BigDecimal voiceExpressiveness,

        String referenceImagePath,

        Long referenceSeed,

        BigDecimal ipadapterWeight
) {
    public CreateCharacterRequest {
        if (role == null) role = CharacterRole.HOST;
        if (ttsVoice == null || ttsVoice.isBlank()) ttsVoice = "ef_dora";
        if (ttsRate == null || ttsRate.isBlank()) ttsRate = "+0%";
        if (ttsPitch == null || ttsPitch.isBlank()) ttsPitch = "default";
        if (voiceEngine == null || voiceEngine.isBlank()) voiceEngine = "KOKORO";
        if (voiceExpressiveness == null) voiceExpressiveness = new BigDecimal("0.50");
        if (ipadapterWeight == null) ipadapterWeight = new BigDecimal("0.85");
    }

    public CreateCharacterRequest(
            String name,
            CharacterRole role,
            String canonicalPrompt,
            String personality,
            String ttsVoice,
            String ttsRate,
            String ttsPitch,
            String referenceImagePath,
            Long referenceSeed,
            BigDecimal ipadapterWeight
    ) {
        this(name, role, canonicalPrompt, personality, ttsVoice, ttsRate, ttsPitch, "KOKORO", null, new BigDecimal("0.50"), referenceImagePath, referenceSeed, ipadapterWeight);
    }
}
