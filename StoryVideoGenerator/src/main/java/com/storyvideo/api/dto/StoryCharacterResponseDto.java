package com.storyvideo.api.dto;

import com.storyvideo.api.domain.StoryCharacter;

import java.util.UUID;

public record StoryCharacterResponseDto(
        UUID id,
        String name,
        String physicalDescription,
        String distinctiveFeatures,
        String role,
        String promptFragment
) {
    public static StoryCharacterResponseDto fromEntity(StoryCharacter character) {
        return new StoryCharacterResponseDto(
                character.getId(),
                character.getName(),
                character.getPhysicalDescription(),
                character.getDistinctiveFeatures(),
                character.getRole(),
                character.getPromptFragment()
        );
    }
}
