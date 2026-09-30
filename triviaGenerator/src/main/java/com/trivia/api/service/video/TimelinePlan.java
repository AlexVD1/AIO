package com.trivia.api.service.video;

import java.util.List;

/**
 * Plan de ensamblado audiovisual completo para el lote de trivias.
 */
public record TimelinePlan(
        List<TriviaSceneTiming> scenes,
        VideoFormat format,
        boolean withBgm
) {
    /**
     * Calcula la duración total teórica del video completo sumando todas las escenas.
     */
    public double getTotalDuration() {
        if (scenes == null || scenes.isEmpty()) {
            return 0.0;
        }
        return scenes.stream().mapToDouble(TriviaSceneTiming::getTotalDuration).sum();
    }
}
