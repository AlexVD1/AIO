package com.kidsanim.api.dto;

import com.kidsanim.api.domain.Series;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SeriesResponse(
        UUID id,
        String name,
        String description,
        String language,
        int targetAgeMin,
        int targetAgeMax,
        String aspectRatio,
        UUID styleProfileId,
        String defaultBgmMood,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static SeriesResponse fromEntity(Series series) {
        if (series == null) return null;
        return new SeriesResponse(
                series.getId(),
                series.getName(),
                series.getDescription(),
                series.getLanguage(),
                series.getTargetAgeMin(),
                series.getTargetAgeMax(),
                series.getAspectRatio(),
                series.getStyleProfile() != null ? series.getStyleProfile().getId() : null,
                series.getDefaultBgmMood(),
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }
}
