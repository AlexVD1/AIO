package com.kidsanim.api.dto;

import java.util.List;
import java.util.UUID;

public record CharacterSheetResponse(
        UUID characterId,
        String characterName,
        List<String> images,
        List<Long> seeds
) {
}
