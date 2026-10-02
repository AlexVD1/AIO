package com.storyvideo.api.dto;

import com.storyvideo.api.domain.*;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;

import java.math.BigDecimal;
import java.util.UUID;

public record StorySceneResponseDto(
        UUID id,
        int sequenceNumber,
        SceneType sceneType,
        String narrationText,
        String narrationEmotion,
        String visualDescription,
        String visualPrompt,
        String imagePath,
        String imageUrl,
        CameraMovement cameraMovement,
        String musicIntensity,
        String ambientSound,
        TransitionType transitionIn,
        TransitionType transitionOut,
        String subtitleStyle,
        BigDecimal estimatedDurationSeconds,
        BigDecimal actualDurationSeconds,
        SceneStatus status
) {
    public static StorySceneResponseDto fromEntity(StoryScene scene, AssetStorageService storageService) {
        String url = (scene.getImagePath() != null && storageService != null)
                ? storageService.buildPublicUrl(scene.getImagePath())
                : null;

        return new StorySceneResponseDto(
                scene.getId(),
                scene.getSequenceNumber(),
                scene.getSceneType(),
                scene.getNarrationText(),
                scene.getNarrationEmotion(),
                scene.getVisualDescription(),
                scene.getVisualPrompt(),
                scene.getImagePath(),
                url,
                scene.getCameraMovement(),
                scene.getMusicIntensity(),
                scene.getAmbientSound(),
                scene.getTransitionIn(),
                scene.getTransitionOut(),
                scene.getSubtitleStyle(),
                scene.getEstimatedDurationSeconds(),
                scene.getActualDurationSeconds(),
                scene.getStatus()
        );
    }
}
