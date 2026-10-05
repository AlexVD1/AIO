package com.kidsanim.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EpisodePipelineStatusResponse(
        UUID episodeId,
        UUID trackingId,
        String title,
        String status,
        String stage,
        int progressPercent,
        String message,
        String finalVideoPath,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
    public static EpisodePipelineStatusResponse initial(UUID episodeId, UUID trackingId, String title) {
        return new EpisodePipelineStatusResponse(
                episodeId,
                trackingId,
                title,
                "PLANNING",
                "PLANNING",
                5,
                "Iniciando planificación del episodio...",
                null,
                null,
                OffsetDateTime.now(),
                null
        );
    }
}
