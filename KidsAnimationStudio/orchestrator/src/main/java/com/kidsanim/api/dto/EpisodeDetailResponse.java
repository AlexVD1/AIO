package com.kidsanim.api.dto;

import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.script.model.EpisodeScript;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EpisodeDetailResponse(
        UUID id,
        UUID seriesId,
        EducationalTopicType topicType,
        String topicDetail,
        String learningObjective,
        String title,
        EpisodeStatus status,
        String fingerprint,
        String finalVideoPath,
        EpisodeScript script,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static EpisodeDetailResponse fromEntity(Episode episode, EpisodeScript script) {
        if (episode == null) return null;
        return new EpisodeDetailResponse(
                episode.getId(),
                episode.getSeries() != null ? episode.getSeries().getId() : null,
                episode.getTopicType(),
                episode.getTopicDetail(),
                episode.getLearningObjective(),
                episode.getTitle(),
                episode.getStatus(),
                episode.getFingerprint(),
                episode.getFinalVideoPath(),
                script,
                episode.getCreatedAt(),
                episode.getUpdatedAt()
        );
    }
}
