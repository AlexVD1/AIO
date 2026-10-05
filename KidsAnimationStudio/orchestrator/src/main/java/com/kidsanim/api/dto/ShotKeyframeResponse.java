package com.kidsanim.api.dto;

import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.ShotStatus;

import java.util.UUID;

public record ShotKeyframeResponse(
        UUID shotId,
        UUID sceneId,
        int orderIndex,
        ShotStatus status,
        ContinuityMode continuityMode,
        String characterName,
        String keyframePath,
        Double similarityScore,
        Long seed,
        int attempts,
        String prompt
) {}
