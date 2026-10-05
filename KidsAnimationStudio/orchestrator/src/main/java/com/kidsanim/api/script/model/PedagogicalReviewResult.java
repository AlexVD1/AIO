package com.kidsanim.api.script.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PedagogicalReviewResult(
        boolean approved,
        List<String> issues
) {
    public PedagogicalReviewResult {
        if (issues == null) issues = new ArrayList<>();
    }

    public static PedagogicalReviewResult ok() {
        return new PedagogicalReviewResult(true, List.of());
    }

    public static PedagogicalReviewResult rejected(List<String> issues) {
        return new PedagogicalReviewResult(false, issues);
    }
}
