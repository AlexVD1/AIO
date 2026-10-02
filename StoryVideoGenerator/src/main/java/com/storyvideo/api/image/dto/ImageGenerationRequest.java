package com.storyvideo.api.image.dto;

import java.util.UUID;

public record ImageGenerationRequest(
        UUID storyId,
        UUID sceneId,
        int sequenceNumber,
        String prompt,
        String negativePrompt,
        Long seed,
        Integer width,
        Integer height,
        Integer steps,
        Double cfgScale,
        String sampler
) {
    public static ImageGenerationRequest simple(UUID storyId, UUID sceneId, int sequenceNumber, String prompt, String negativePrompt, Long seed) {
        return new ImageGenerationRequest(
                storyId,
                sceneId,
                sequenceNumber,
                prompt,
                negativePrompt,
                seed,
                768,
                1344,
                25,
                7.0,
                "DPM++ 2M Karras"
        );
    }

    public int resolvedWidth() {
        return (width != null && width > 0) ? width : 768;
    }

    public int resolvedHeight() {
        return (height != null && height > 0) ? height : 1344;
    }

    public int resolvedSteps() {
        return (steps != null && steps > 0) ? steps : 25;
    }

    public double resolvedCfgScale() {
        return (cfgScale != null && cfgScale > 0) ? cfgScale : 7.0;
    }

    public String resolvedSampler() {
        return (sampler != null && !sampler.isBlank()) ? sampler : "DPM++ 2M Karras";
    }

    public long resolvedSeed() {
        return seed != null ? seed : -1L;
    }
}
