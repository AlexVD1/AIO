package com.trivia.api.dto;

import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.EstadoTrivia;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TriviaOpcion;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTO detallado para la consulta de una trivia almacenada en el sistema.
 */
public record TriviaResponse(
        UUID id,
        String tipoTrivia,
        String subtema,
        String pregunta,
        String explicacion,
        Dificultad dificultad,
        String idioma,
        EstadoTrivia estado,
        List<TriviaOpcionResponse> opciones,
        List<TriviaAssetResponse> assets,
        LocalDateTime createdAt
) {
    public static TriviaResponse from(Trivia t, List<TriviaOpcion> opciones, List<TriviaAsset> assets) {
        List<TriviaOpcionResponse> opcResponses = opciones != null
                ? opciones.stream().map(TriviaOpcionResponse::from).toList()
                : Collections.emptyList();

        List<TriviaAssetResponse> assetResponses = assets != null
                ? assets.stream().map(TriviaAssetResponse::from).toList()
                : Collections.emptyList();

        return new TriviaResponse(
                t.getId(),
                t.getTipoTrivia().getCodigo(),
                t.getSubtema(),
                t.getPregunta(),
                t.getExplicacion(),
                t.getDificultad(),
                t.getIdioma(),
                t.getEstado(),
                opcResponses,
                assetResponses,
                t.getCreatedAt()
        );
    }
}
