package com.kidsanim.api.dto;

import com.kidsanim.api.domain.BatchJob;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.enums.BatchJobStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BatchResponse(
        UUID id,
        UUID seriesId,
        String name,
        BatchJobStatus status,
        int requestedCount,
        int completedCount,
        int failedCount,
        List<UUID> episodeIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static BatchResponse fromEntity(BatchJob job) {
        List<UUID> epIds = job.getEpisodes() != null
                ? job.getEpisodes().stream().map(Episode::getId).toList()
                : List.of();
        UUID sId = job.getSeries() != null ? job.getSeries().getId() : null;
        return new BatchResponse(
                job.getId(),
                sId,
                job.getName(),
                job.getStatus(),
                job.getRequestedCount(),
                job.getCompletedCount(),
                job.getFailedCount(),
                epIds,
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
