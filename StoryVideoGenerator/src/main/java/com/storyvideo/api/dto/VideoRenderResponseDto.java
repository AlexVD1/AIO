package com.storyvideo.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VideoRenderResponseDto(
        UUID renderId,
        UUID videoProjectId,
        UUID storyId,
        int attemptNumber,
        String status,
        String videoUrl,
        double durationSeconds,
        String resolution,
        String codec,
        boolean valid,
        List<String> validationIssues,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {}
