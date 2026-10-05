package com.kidsanim.api.script.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShotScript(
        String speaker,
        String character,
        String narration,
        String visualPrompt,
        String actionPrompt,
        CameraMotion cameraMotion,
        ContinuityMode continuityMode,
        List<OverlaySpec> overlays,
        List<String> sfx,
        Integer pauseAfterMs
) {
    public ShotScript {
        if (speaker == null || speaker.isBlank()) speaker = "NARRATOR";
        if (cameraMotion == null) cameraMotion = CameraMotion.STATIC;
        if (continuityMode == null) continuityMode = ContinuityMode.NEW_KEYFRAME;
        if (overlays == null) overlays = new ArrayList<>();
        if (sfx == null) sfx = new ArrayList<>();
        if (pauseAfterMs == null) pauseAfterMs = 300;
    }
}
