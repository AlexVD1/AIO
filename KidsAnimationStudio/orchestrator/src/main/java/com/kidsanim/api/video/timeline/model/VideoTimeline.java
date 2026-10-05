package com.kidsanim.api.video.timeline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record VideoTimeline(
        UUID episodeId,
        double totalDurationSeconds,
        int width,
        int height,
        int fps,
        List<TimelineEntry> entries,
        AudioMixPlan audioMix
) {
    public VideoTimeline {
        if (entries == null) entries = new ArrayList<>();
        if (width <= 0) width = 1920;
        if (height <= 0) height = 1080;
        if (fps <= 0) fps = 30;
    }
}
