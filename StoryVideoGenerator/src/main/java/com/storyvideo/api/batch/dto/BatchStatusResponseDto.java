package com.storyvideo.api.batch.dto;

import com.storyvideo.api.domain.BatchJobStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BatchStatusResponseDto(
        UUID batchId,
        BatchJobStatus status,
        int totalRequested,
        int totalCompleted,
        int totalFailed,
        int progressPercent,
        Map<String, Integer> genreDistribution,
        List<BatchStoryItemDto> stories,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
