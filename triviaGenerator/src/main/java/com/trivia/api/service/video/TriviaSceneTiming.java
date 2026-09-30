package com.trivia.api.service.video;

import java.util.UUID;

/**
 * Representa la planificación de tiempos y recursos de audio/video para una trivia individual.
 *
 * Esta estructura desacopla completamente el motor de renderizado de la presencia o ausencia
 * de locución (TTS). En el futuro, cuando se genere audio con voz, simplemente se asignan
 * las duraciones calculadas a partir del archivo de audio real.
 */
public record TriviaSceneTiming(
        UUID triviaId,
        int index,
        int total,
        String headerText,
        double questionDuration,
        double countdownDuration,
        double answerDuration,
        String questionAudioPath,
        String answerAudioPath
) {
    /**
     * Duración total de la escena de la trivia individual en segundos.
     */
    public double getTotalDuration() {
        return questionDuration + countdownDuration + answerDuration;
    }
}
