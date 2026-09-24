package com.trivia.api.ai.dto;

import java.util.List;

/**
 * Representa una trivia individual generada por el modelo de IA.
 */
public record AiTriviaItem(
        String pregunta,
        List<AiTriviaOpcion> opciones,
        String explicacion
) {
}
