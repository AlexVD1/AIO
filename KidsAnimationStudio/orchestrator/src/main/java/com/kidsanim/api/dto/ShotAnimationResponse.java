package com.kidsanim.api.dto;

import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.ShotStatus;

import java.util.UUID;

public record ShotAnimationResponse(
        UUID shotId,
        UUID sceneId,
        int orderIndex,
        ShotStatus status,
        ContinuityMode continuityMode,
        String clipPath,
        String lastFramePath,
        int frames,
        int durationMs,
        Long seed,
        int attempts,
        String actionPrompt
) {}
