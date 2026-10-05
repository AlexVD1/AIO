package com.kidsanim.api.video.timeline.model;

import java.util.ArrayList;
import java.util.List;

public record AudioMixPlan(
        String musicTrackPath,
        double musicBaseVolume,
        List<VolumeDuckPoint> duckingPoints,
        List<SfxCue> globalSfx
) {
    public AudioMixPlan {
        if (musicBaseVolume <= 0) musicBaseVolume = 0.12;
        if (duckingPoints == null) duckingPoints = new ArrayList<>();
        if (globalSfx == null) globalSfx = new ArrayList<>();
    }
}
