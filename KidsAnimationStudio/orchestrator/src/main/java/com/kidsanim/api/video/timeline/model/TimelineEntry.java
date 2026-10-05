package com.kidsanim.api.video.timeline.model;

import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.script.model.OverlaySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TimelineEntry(
        UUID shotId,
        int sequenceNumber,
        int sceneOrderIndex,
        int shotOrderIndex,
        double startTime,
        double endTime,
        double duration,
        String clipPath,
        String keyframeImagePath,
        CameraMotion cameraMotion,
        ContinuityMode continuityMode,
        String narrationText,
        String narrationAudioPath,
        double narrationStartOffset,
        double narrationDuration,
        List<OverlaySpec> overlays,
        List<SfxCue> sfxCues
) {
    public TimelineEntry {
        if (cameraMotion == null) cameraMotion = CameraMotion.STATIC;
        if (continuityMode == null) continuityMode = ContinuityMode.NEW_KEYFRAME;
        if (overlays == null) overlays = new ArrayList<>();
        if (sfxCues == null) sfxCues = new ArrayList<>();
    }
}
