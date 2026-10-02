package com.storyvideo.api.video.timeline.model;

import java.util.List;
import java.util.UUID;

public record VideoTimeline(
        UUID storyId,
        double totalDurationSeconds,
        List<TimelineEntry> entries,
        AudioMixPlan audioMix
) {
    public VideoTimeline {
        if (entries == null) entries = List.of();
    }
}
