package com.trivia.api.dto;

import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.EstadoGeneracion;
import com.trivia.api.domain.TriviaGeneration;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta que representa el estado y resultado de una solicitud de generación de trivias.
 */
public record TriviaGenerationResponse(
        UUID id,
        String tipoTrivia,
        String subtema,
        Integer cantidadSolicitada,
        Integer cantidadGenerada,
        Integer cantidadDescartada,
        Integer cantidadFinal,
        Integer numeroOpciones,
        Dificultad dificultad,
        String idioma,
        Integer intentos,
        EstadoGeneracion estado,
        String error,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static TriviaGenerationResponse from(TriviaGeneration g) {
        return new TriviaGenerationResponse(
                g.getId(),
                g.getTipoTrivia().getCodigo(),
                g.getSubtema(),
                g.getCantidadSolicitada(),
                g.getCantidadGenerada(),
                g.getCantidadDescartada(),
                g.getCantidadFinal(),
                g.getNumeroOpciones(),
                g.getDificultad(),
                g.getIdioma(),
                g.getIntentos(),
                g.getEstado(),
                g.getError(),
                g.getCreatedAt(),
                g.getCompletedAt()
        );
    }
}
