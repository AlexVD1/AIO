package com.trivia.api.ai.dto;

/**
 * Representa una opción de respuesta generada por el modelo de IA.
 */
public record AiTriviaOpcion(
        String letra,
        String texto,
        Boolean correcta
) {
}
