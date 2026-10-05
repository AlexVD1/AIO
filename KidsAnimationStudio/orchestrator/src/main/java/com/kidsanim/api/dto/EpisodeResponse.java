package com.kidsanim.api.dto;

import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EpisodeResponse(
        UUID id,
        UUID seriesId,
        EducationalTopicType topicType,
        String topicDetail,
        String learningObjective,
        String title,
        EpisodeStatus status,
        String fingerprint,
        int sceneCount,
        int shotCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static EpisodeResponse fromEntity(Episode episode) {
        if (episode == null) return null;
        int scenes = episode.getScenes() != null ? episode.getScenes().size() : 0;
        int shots = episode.getScenes() != null
                ? episode.getScenes().stream().mapToInt(s -> s.getShots() != null ? s.getShots().size() : 0).sum()
                : 0;

        return new EpisodeResponse(
                episode.getId(),
                episode.getSeries() != null ? episode.getSeries().getId() : null,
                episode.getTopicType(),
                episode.getTopicDetail(),
                episode.getLearningObjective(),
                episode.getTitle(),
                episode.getStatus(),
                episode.getFingerprint(),
                scenes,
                shots,
                episode.getCreatedAt(),
                episode.getUpdatedAt()
        );
    }
}
