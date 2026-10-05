package com.kidsanim.api.dto;

import com.kidsanim.api.domain.Location;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        UUID seriesId,
        String name,
        String prompt,
        String referenceImagePath,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static LocationResponse fromEntity(Location location) {
        if (location == null) return null;
        return new LocationResponse(
                location.getId(),
                location.getSeries() != null ? location.getSeries().getId() : null,
                location.getName(),
                location.getPrompt(),
                location.getReferenceImagePath(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}
