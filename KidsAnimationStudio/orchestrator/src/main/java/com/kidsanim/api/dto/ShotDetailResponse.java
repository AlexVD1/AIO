package com.kidsanim.api.dto;

import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.ShotStatus;

import java.util.UUID;

public record ShotDetailResponse(
        UUID id,
        UUID sceneId,
        int orderIndex,
        String narrationText,
        String visualPrompt,
        String actionPrompt,
        String speaker,
        ShotStatus status,
        ContinuityMode continuityMode,
        CameraMotion cameraMotion,
        String characterName,
        UUID characterId,
        Double similarityScore,
        Long seed,
        int attempts,
        int targetDurationMs,
        int pauseAfterMs
) {
    public static ShotDetailResponse fromEntity(Shot shot) {
        if (shot == null) return null;
        return new ShotDetailResponse(
                shot.getId(),
                shot.getScene() != null ? shot.getScene().getId() : null,
                shot.getOrderIndex(),
                shot.getNarrationText(),
                shot.getVisualPrompt(),
                shot.getActionPrompt(),
                shot.getSpeaker(),
                shot.getStatus(),
                shot.getContinuityMode(),
                shot.getCameraMotion(),
                shot.getCharacter() != null ? shot.getCharacter().getName() : null,
                shot.getCharacter() != null ? shot.getCharacter().getId() : null,
                shot.getSimilarityScore() != null ? shot.getSimilarityScore().doubleValue() : null,
                shot.getSeed(),
                shot.getAttempts(),
                shot.getTargetDurationMs(),
                shot.getPauseAfterMs()
        );
    }
}
