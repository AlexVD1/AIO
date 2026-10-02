package com.storyvideo.api.pipeline.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PipelineStatusDto(
        UUID storyId,
        String status,
        PipelineStage currentStage,
        int progressPercent,
        String message,
        String videoUrl,
        String exportPath,
        List<String> errors,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public PipelineStatusDto {
        if (errors == null) errors = List.of();
    }

    public static PipelineStatusDto initial(UUID storyId) {
        return new PipelineStatusDto(
                storyId,
                "INITIALIZED",
                PipelineStage.NOT_STARTED,
                0,
                "Pipeline encolado para ejecución",
                null,
                null,
                List.of(),
                LocalDateTime.now(),
                null
        );
    }
}
