package com.storyvideo.api.audio.narration.dto;

import java.util.UUID;

public record NarrationRequest(
        UUID storyId,
        UUID sceneId,
        int sequenceNumber,
        String text,
        String voice,
        String emotion,
        String rate,
        String pitch
) {
    public static NarrationRequest simple(UUID storyId, UUID sceneId, int sequenceNumber, String text, String voice) {
        return new NarrationRequest(
                storyId,
                sceneId,
                sequenceNumber,
                text,
                (voice != null && !voice.isBlank()) ? voice : "es-MX-JorgeNeural",
                "NEUTRAL",
                "+0%",
                "+0Hz"
        );
    }

    public String resolvedVoice() {
        return (voice != null && !voice.isBlank()) ? voice : "es-MX-JorgeNeural";
    }

    public String resolvedRate() {
        return (rate != null && !rate.isBlank()) ? rate : "+0%";
    }

    public String resolvedPitch() {
        return (pitch != null && !pitch.isBlank()) ? pitch : "+0Hz";
    }
}
