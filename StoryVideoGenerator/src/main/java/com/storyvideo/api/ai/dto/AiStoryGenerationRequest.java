package com.storyvideo.api.ai.dto;

import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryTone;

import java.util.List;

public record AiStoryGenerationRequest(
        StoryGenre genre,
        StoryTone tone,
        String theme,
        int targetDurationSeconds,
        String language,
        int sceneCount,
        List<String> recentPremisesToAvoid,
        String visualStyleGuidance
) {
    public static AiStoryGenerationRequest of(
            StoryGenre genre, StoryTone tone, String theme, int targetDurationSeconds, String language, int sceneCount) {
        return new AiStoryGenerationRequest(genre, tone, theme, targetDurationSeconds, language, sceneCount, List.of(), null);
    }
}
