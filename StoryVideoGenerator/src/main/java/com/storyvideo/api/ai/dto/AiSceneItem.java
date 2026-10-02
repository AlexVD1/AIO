package com.storyvideo.api.ai.dto;

import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.SceneType;
import com.storyvideo.api.domain.TransitionType;

public record AiSceneItem(
        int sequenceNumber,
        SceneType sceneType,
        String narrationText,
        String narrationEmotion,
        String visualDescription,
        String visualPrompt,
        CameraMovement cameraMovement,
        String musicIntensity,
        String ambientSound,
        TransitionType transitionIn,
        TransitionType transitionOut
) {}
