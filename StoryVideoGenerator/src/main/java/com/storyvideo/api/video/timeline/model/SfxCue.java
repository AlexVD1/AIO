package com.storyvideo.api.video.timeline.model;

public record SfxCue(
        String sfxType,
        String sfxPath,
        double timestampSeconds,
        double volume
) {}
