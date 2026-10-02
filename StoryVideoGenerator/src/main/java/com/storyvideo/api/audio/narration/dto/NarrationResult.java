package com.storyvideo.api.audio.narration.dto;

public record NarrationResult(
        byte[] audioBytes,
        double durationSeconds,
        String audioFormat,
        String voiceUsed
) {
    public static NarrationResult mp3(byte[] bytes, double durationSeconds, String voiceUsed) {
        return new NarrationResult(bytes, durationSeconds, "audio/mpeg", voiceUsed);
    }
}
