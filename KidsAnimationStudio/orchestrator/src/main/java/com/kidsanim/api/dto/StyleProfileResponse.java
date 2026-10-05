package com.kidsanim.api.dto;

import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.VideoModel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StyleProfileResponse(
        UUID id,
        String name,
        String checkpointName,
        String stylePrompt,
        String negativePrompt,
        String sampler,
        int steps,
        BigDecimal cfg,
        int width,
        int height,
        VideoModel videoModel,
        int videoFps,
        String videoResolution,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static StyleProfileResponse fromEntity(StyleProfile profile) {
        if (profile == null) return null;
        return new StyleProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getCheckpointName(),
                profile.getStylePrompt(),
                profile.getNegativePrompt(),
                profile.getSampler(),
                profile.getSteps(),
                profile.getCfg(),
                profile.getWidth(),
                profile.getHeight(),
                profile.getVideoModel(),
                profile.getVideoFps(),
                profile.getVideoResolution(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
