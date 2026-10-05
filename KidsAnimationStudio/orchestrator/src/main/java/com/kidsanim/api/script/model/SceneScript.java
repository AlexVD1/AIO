package com.kidsanim.api.script.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kidsanim.api.domain.enums.ScenePurpose;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SceneScript(
        ScenePurpose purpose,
        String location,
        String bgmMood,
        List<ShotScript> shots
) {
    public SceneScript {
        if (purpose == null) purpose = ScenePurpose.INTRO;
        if (location == null || location.isBlank()) location = "default";
        if (bgmMood == null || bgmMood.isBlank()) bgmMood = "playful";
        if (shots == null) shots = new ArrayList<>();
    }
}
