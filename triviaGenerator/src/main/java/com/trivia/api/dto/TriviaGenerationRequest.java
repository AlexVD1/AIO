package com.trivia.api.dto;

import com.trivia.api.domain.Dificultad;
import jakarta.validation.constraints.*;

/**
 * Solicitud de generación de contenido de trivia.
 *
 * Contiene todas las validaciones de entrada (Bean Validation)
 * requeridas para garantizar parámetros válidos antes de iniciar el pipeline.
 */
public record TriviaGenerationRequest(
        @NotBlank(message = "El tipo de trivia es obligatorio (ej. ASTRONOMIA, HISTORIA)")
        String tipoTrivia,

        @Size(max = 200, message = "El subtema no puede exceder 200 caracteres")
        String subtema,

        @NotNull(message = "La cantidad solicitada es obligatoria")
        @Min(value = 1, message = "La cantidad mínima a generar es 1")
        @Max(value = 50, message = "La cantidad máxima por solicitud es 50")
        Integer cantidad,

        @NotNull(message = "El número de opciones es obligatorio")
        @Min(value = 2, message = "El número mínimo de opciones es 2")
        @Max(value = 8, message = "El número máximo de opciones es 8")
        Integer numeroOpciones,

        @NotNull(message = "La dificultad es obligatoria (FACIL, MEDIA, DIFICIL)")
        Dificultad dificultad,

        @NotBlank(message = "El idioma es obligatorio (ej. es-MX, es, en)")
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "El idioma debe seguir formato ISO/BCP47 como 'es' o 'es-MX'")
        String idioma
) {
}
