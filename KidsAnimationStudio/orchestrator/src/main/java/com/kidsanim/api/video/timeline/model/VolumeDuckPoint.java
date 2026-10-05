package com.kidsanim.api.video.timeline.model;

public record VolumeDuckPoint(
        double startTime,
        double endTime,
        double duckVolume
) {
    public VolumeDuckPoint {
        if (duckVolume <= 0) duckVolume = 0.05;
    }
}
