package com.storyvideo.api.ai.dto;

import com.storyvideo.api.domain.NarrativeArc;

import java.util.List;

public record AiStoryResponse(
        String title,
        String hook,
        String premise,
        String synopsis,
        NarrativeArc narrativeArc,
        String twist,
        String ending,
        List<AiCharacterItem> characters,
        List<AiSceneItem> scenes
) {}
