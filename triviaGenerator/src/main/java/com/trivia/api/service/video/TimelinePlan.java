package com.trivia.api.service.video;

import java.util.List;

/**
 * Plan de ensamblado audiovisual completo para el lote de trivias,
 * incluyendo opcionalmente la escena de introducción temática.
 */
public record TimelinePlan(
        VideoIntroTiming introTiming,
        List<TriviaSceneTiming> scenes,
        VideoFormat format,
        boolean withBgm
) {
    public TimelinePlan(List<TriviaSceneTiming> scenes, VideoFormat format, boolean withBgm) {
        this(null, scenes, format, withBgm);
    }

    /**
     * Determina si el plan audiovisual incluye una escena de introducción con contenido válido.
     */
    public boolean hasIntro() {
        return introTiming != null && introTiming.hasContent();
    }

    /**
     * Calcula la duración total teórica del video completo sumando todas las escenas y la introducción.
     */
    public double getTotalDuration() {
        double introDuration = (introTiming != null) ? introTiming.duration() : 0.0;
        if (scenes == null || scenes.isEmpty()) {
            return introDuration;
        }
        return introDuration + scenes.stream().mapToDouble(TriviaSceneTiming::getTotalDuration).sum();
    }
}
