package com.kidsanim.api.dto;

public record GenerateSheetRequest(
        Long seed,
        Integer count,
        Integer width,
        Integer height
) {
    public GenerateSheetRequest {
        if (count == null || count <= 0) count = 4;
    }
}
