package com.storyvideo.api.video.validation.dto;

import java.util.List;

public record VideoValidationResult(
        boolean valid,
        List<String> issues,
        double actualDuration,
        String videoCodec,
        String audioCodec,
        int width,
        int height,
        long fileSizeBytes,
        String rawProbeJson
) {
    public VideoValidationResult {
        if (issues == null) issues = List.of();
    }
}
