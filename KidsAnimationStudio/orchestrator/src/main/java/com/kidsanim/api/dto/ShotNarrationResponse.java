package com.kidsanim.api.dto;

import com.kidsanim.api.gateway.AiGatewayClient.WordTimestamp;

import java.util.List;
import java.util.UUID;

public record ShotNarrationResponse(
        UUID shotId,
        UUID sceneId,
        int orderIndex,
        String speaker,
        String voice,
        String narrationText,
        int audioDurationMs,
        int targetDurationMs,
        String audioPath,
        List<WordTimestamp> wordTimestamps
) {}
