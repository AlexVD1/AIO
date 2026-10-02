package com.storyvideo.api.video.timeline.model;

public record VolumeDuckPoint(
        double startTime,
        double endTime,
        double duckedVolume
) {}
