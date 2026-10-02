package com.trivia.api.dto;

import com.trivia.api.service.video.VideoIntroMode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta con el texto de introducción resuelto y el tema detectado.
 */
@Schema(description = "Resultado de la resolución o previsualización de la introducción")
public record VideoIntroPreviewResponse(

        @Schema(description = "Modalidad solicitada", example = "TEMPLATE")
        VideoIntroMode mode,

        @Schema(description = "Tema o subtema detectado o utilizado", example = "Historia romana")
        String topic,

        @Schema(description = "Texto final de la introducción generado o resuelto", example = "Pon a prueba tus conocimientos sobre Historia romana.")
        String introText
) {}
