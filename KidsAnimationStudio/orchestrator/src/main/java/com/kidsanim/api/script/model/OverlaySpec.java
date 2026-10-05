package com.kidsanim.api.script.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OverlaySpec(
        String type,
        String value,
        String position,
        String appearAtWord
) {
    public OverlaySpec {
        if (position == null || position.isBlank()) position = "TOP_RIGHT";
    }
}
