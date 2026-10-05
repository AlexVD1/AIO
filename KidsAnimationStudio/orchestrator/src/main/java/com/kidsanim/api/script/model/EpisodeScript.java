package com.kidsanim.api.script.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EpisodeScript(
        String title,
        String learningObjective,
        List<SceneScript> scenes
) {
    public EpisodeScript {
        if (title == null) title = "Episodio sin título";
        if (learningObjective == null) learningObjective = "";
        if (scenes == null) scenes = new ArrayList<>();
    }

    public int totalShots() {
        return scenes.stream()
                .mapToInt(s -> s.shots() != null ? s.shots().size() : 0)
                .sum();
    }
}
