package com.kidsanim.api.dto;

import java.util.UUID;

public record EpisodeVideoResponse(
        UUID episodeId,
        String title,
        String status,
        String videoPath,
        Double durationSeconds,
        Boolean existsOnDisk,
        Long fileSizeBytes
) {}
