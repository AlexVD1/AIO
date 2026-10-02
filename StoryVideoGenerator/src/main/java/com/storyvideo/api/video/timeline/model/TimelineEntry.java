package com.storyvideo.api.video.timeline.model;

import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.TransitionType;

import java.util.List;
import java.util.UUID;

public record TimelineEntry(
        UUID sceneId,
        int sequenceNumber,
        double startTime,
        double endTime,
        double duration,
        String imagePath,
        CameraMovement cameraMovement,
        TransitionType transitionIn,
        TransitionType transitionOut,
        double transitionDuration,
        List<String> visualEffects,
        String narrationAudioPath,
        double narrationStartOffset,
        double narrationDuration,
        List<SubtitleCue> subtitles,
        List<SfxCue> sfxCues
) {
    public TimelineEntry {
        if (visualEffects == null) visualEffects = List.of();
        if (subtitles == null) subtitles = List.of();
        if (sfxCues == null) sfxCues = List.of();
        if (cameraMovement == null) cameraMovement = CameraMovement.SLOW_ZOOM_IN;
        if (transitionIn == null) transitionIn = TransitionType.CUT;
        if (transitionOut == null) transitionOut = TransitionType.CUT;
    }
}
