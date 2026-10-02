package com.storyvideo.api.video.timeline.model;

public record SubtitleCue(
        int index,
        double startTime,
        double endTime,
        String text
) {}
