package com.trivia.api.dto;

import com.trivia.api.domain.TriviaOpcion;

import java.util.UUID;

/**
 * DTO para representar una opción de respuesta al consultar una trivia.
 */
public record TriviaOpcionResponse(
        UUID id,
        String letra,
        String texto,
        Boolean correcta
) {
    public static TriviaOpcionResponse from(TriviaOpcion o) {
        return new TriviaOpcionResponse(
                o.getId(),
                o.getLetra(),
                o.getTexto(),
                o.getCorrecta()
        );
    }
}
