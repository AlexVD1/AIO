package com.storyvideo.api.video.timeline.model;

import java.util.List;

public record AudioMixPlan(
        String musicTrackPath,
        double musicBaseVolume,
        List<VolumeDuckPoint> duckingPoints,
        List<SfxCue> globalSfx
) {
    public AudioMixPlan {
        if (duckingPoints == null) duckingPoints = List.of();
        if (globalSfx == null) globalSfx = List.of();
    }
}
