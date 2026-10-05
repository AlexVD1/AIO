package com.kidsanim.api.dto;

import com.kidsanim.api.domain.Series;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SeriesDetailResponse(
        UUID id,
        String name,
        String description,
        String language,
        int targetAgeMin,
        int targetAgeMax,
        String aspectRatio,
        StyleProfileResponse styleProfile,
        String defaultBgmMood,
        int characterCount,
        int locationCount,
        int episodeCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static SeriesDetailResponse fromEntity(Series series) {
        if (series == null) return null;
        return new SeriesDetailResponse(
                series.getId(),
                series.getName(),
                series.getDescription(),
                series.getLanguage(),
                series.getTargetAgeMin(),
                series.getTargetAgeMax(),
                series.getAspectRatio(),
                StyleProfileResponse.fromEntity(series.getStyleProfile()),
                series.getDefaultBgmMood(),
                series.getCharacters() != null ? series.getCharacters().size() : 0,
                series.getLocations() != null ? series.getLocations().size() : 0,
                series.getEpisodes() != null ? series.getEpisodes().size() : 0,
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }
}
