package com.trivia.api.service.video;

/**
 * Planificación de tiempos y recursos multimedia para la escena de introducción.
 */
public record VideoIntroTiming(
        String introText,
        String topic,
        double duration,
        String audioPath
) {
    /**
     * Determina si la introducción contiene texto válido para ser renderizada en el video.
     */
    public boolean hasContent() {
        return introText != null && !introText.isBlank();
    }
}
