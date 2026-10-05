package com.kidsanim.api.export.dto;

import java.util.UUID;

public record ExportResult(
        UUID episodeId,
        String exportedVideoPath,
        String thumbnailPath,
        String metadataPath,
        boolean success,
        String message
) {}
