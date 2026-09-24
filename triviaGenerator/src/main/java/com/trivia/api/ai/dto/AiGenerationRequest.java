package com.trivia.api.ai.dto;

import com.trivia.api.domain.Dificultad;

import java.util.Collections;
import java.util.List;

/**
 * Solicitud de generación de contenido hacia el cliente de Inteligencia Artificial.
 *
 * Contiene todos los parámetros necesarios para que el LLM genere trivias temáticas,
 * incluyendo el contexto de preguntas ya existentes en la BD para evitar repetirlas.
 */
public record AiGenerationRequest(
        String tipoTrivia,
        String subtema,
        Dificultad dificultad,
        String idioma,
        int numeroOpciones,
        int cantidad,
        List<String> preguntasExistentes
) {
    public AiGenerationRequest {
        if (preguntasExistentes == null) {
            preguntasExistentes = Collections.emptyList();
        }
    }
}
