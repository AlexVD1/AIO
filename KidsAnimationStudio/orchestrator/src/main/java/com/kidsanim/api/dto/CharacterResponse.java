package com.kidsanim.api.dto;

import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.CharacterStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CharacterResponse(
        UUID id,
        UUID seriesId,
        String name,
        CharacterRole role,
        String canonicalPrompt,
        String personality,
        String ttsVoice,
        String ttsRate,
        String ttsPitch,
        String referenceImagePath,
        Long referenceSeed,
        BigDecimal ipadapterWeight,
        CharacterStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CharacterResponse fromEntity(Character character) {
        if (character == null) return null;
        return new CharacterResponse(
                character.getId(),
                character.getSeries() != null ? character.getSeries().getId() : null,
                character.getName(),
                character.getRole(),
                character.getCanonicalPrompt(),
                character.getPersonality(),
                character.getTtsVoice(),
                character.getTtsRate(),
                character.getTtsPitch(),
                character.getReferenceImagePath(),
                character.getReferenceSeed(),
                character.getIpadapterWeight(),
                character.getStatus(),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }
}
