package com.kidsanim.api.video.timeline.model;

public record SfxCue(
        String name,
        String sfxPath,
        double timestampSeconds,
        double volume
) {
    public SfxCue {
        if (name == null || name.isBlank()) name = "sfx";
        if (volume <= 0) volume = 0.70;
    }
}
