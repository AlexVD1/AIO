package com.kidsanim.api.video.validation.dto;

import java.util.List;

public record VideoValidationResult(
        boolean valid,
        List<String> issues,
        double durationSeconds,
        String videoCodec,
        String audioCodec,
        int width,
        int height,
        long fileSizeBytes,
        String rawProbeJson
) {}
