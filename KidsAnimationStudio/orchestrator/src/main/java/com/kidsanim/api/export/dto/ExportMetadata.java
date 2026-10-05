package com.kidsanim.api.export.dto;

import java.util.List;
import java.util.UUID;

public record ExportMetadata(
        UUID episodeId,
        String title,
        String seriesName,
        String topicType,
        String topicDetail,
        String learningObjective,
        List<String> tags,
        String category,
        String language,
        String targetAge,
        boolean madeForKids,
        boolean containsSyntheticMedia,
        double durationSeconds,
        String resolution,
        String videoFile,
        String thumbnailFile,
        String createdAt
) {}
