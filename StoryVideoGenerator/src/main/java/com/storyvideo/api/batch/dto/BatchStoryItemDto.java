package com.storyvideo.api.batch.dto;

import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record BatchStoryItemDto(
        UUID storyId,
        String title,
        StoryGenre genre,
        StoryStatus status,
        String videoUrl,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
