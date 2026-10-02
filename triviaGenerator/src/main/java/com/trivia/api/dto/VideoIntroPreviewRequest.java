package com.trivia.api.dto;

import com.trivia.api.service.video.VideoIntroMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Petición para resolver y previsualizar el texto de introducción antes de compilar el video.
 */
@Schema(description = "Solicitud para previsualizar o generar el texto de la introducción")
public record VideoIntroPreviewRequest(

        @Schema(description = "Modalidad de introducción", example = "TEMPLATE", defaultValue = "TEMPLATE")
        VideoIntroMode mode,

        @Schema(description = "Tema o subtema explícito (opcional, si se omite se infiere de las trivias)", example = "Historia romana")
        String topic,

        @Schema(description = "Plantilla seleccionada o ID de plantilla", example = "¿Qué tanto sabes de {tema}?")
        String template,

        @Schema(description = "Texto personalizado en caso de modalidad CUSTOM", example = "¿Eres realmente un experto en Interstellar?")
        String customText,

        @Schema(description = "Lista opcional de IDs de trivias para inferir el tema automáticamente")
        List<UUID> triviaIds,

        @Schema(description = "Código de idioma para generación con IA", example = "es-MX", defaultValue = "es-MX")
        String idioma
) {
    public VideoIntroPreviewRequest {
        if (mode == null) {
            mode = VideoIntroMode.TEMPLATE;
        }
        if (idioma == null || idioma.isBlank()) {
            idioma = "es-MX";
        }
    }
}
