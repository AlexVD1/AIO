package com.trivia.api.dto;

import com.trivia.api.service.video.VideoFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta del proceso de generación de video de trivias.
 */
@Schema(description = "Resultado de la generación de video de trivias")
public record VideoGenerationResponse(

        @Schema(description = "Identificador único asignado al video", example = "a7b3c2d1-e5f6-4a7b-8c9d-0e1f2a3b4c5d")
        UUID id,

        @Schema(description = "Estado de la generación (COMPLETED, FAILED, PROCESSING)", example = "COMPLETED")
        String status,

        @Schema(description = "URL pública para acceder o descargar el archivo MP4",
                example = "http://localhost:8080/assets/videos/a7b3c2d1-e5f6-4a7b-8c9d-0e1f2a3b4c5d.mp4")
        String videoUrl,

        @Schema(description = "Cantidad de preguntas/trivias compiladas", example = "10")
        int totalTrivias,

        @Schema(description = "Duración total aproximada del video en segundos", example = "130.0")
        double durationSeconds,

        @Schema(description = "Formato de resolución del video", example = "VERTICAL_9_16")
        VideoFormat format,

        @Schema(description = "Fecha y hora de generación")
        LocalDateTime createdAt
) {}
