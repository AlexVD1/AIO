package com.storyvideo.api.ai.dto;

public record AiCharacterItem(
        String name,
        String physicalDescription,
        String distinctiveFeatures,
        String role,
        String promptFragment
) {}
