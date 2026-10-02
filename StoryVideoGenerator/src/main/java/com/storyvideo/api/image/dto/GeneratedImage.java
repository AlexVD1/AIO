package com.storyvideo.api.image.dto;

public record GeneratedImage(
        byte[] imageBytes,
        String mimeType,
        long seedUsed,
        int width,
        int height,
        long durationMillis
) {
    public static GeneratedImage png(byte[] bytes, long seed, int width, int height, long durationMillis) {
        return new GeneratedImage(bytes, "image/png", seed, width, height, durationMillis);
    }
}
