package com.trivia.api.dto;

import com.trivia.api.service.video.VideoFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Solicitud de generación de video recopilatorio para un conjunto de trivias.
 */
@Schema(description = "Solicitud para compilar múltiples trivias en un video MP4")
public record VideoGenerationRequest(

        @Schema(description = "Lista de identificadores UUID de las trivias que formarán el video (ordenadas)",
                example = "[\"30a3d4db-7829-4541-a133-2e3e2285a8a9\", \"f9e8ba64-5b23-4ffa-97c2-7fcce144431d\"]")
        @NotEmpty(message = "Debe proporcionar al menos un ID de trivia para generar el video")
        List<UUID> triviaIds,

        @Schema(description = "Formato de video y relación de aspecto (VERTICAL_9_16 para Shorts/TikTok, SQUARE_1_1 para 1:1, HORIZONTAL_16_9 para YouTube)",
                example = "VERTICAL_9_16",
                defaultValue = "VERTICAL_9_16")
        VideoFormat format,

        @Schema(description = "Indica si se incluye música de fondo en loop",
                example = "true",
                defaultValue = "true")
        Boolean withBgm,

        @Schema(description = "Indica si se activa la narración de voz con IA (TTS) para preguntas y respuestas",
                example = "true",
                defaultValue = "false")
        Boolean withTts,

        @Schema(description = "Voz neuronal a utilizar para TTS (ej. 'es-MX-JorgeNeural', 'es-MX-DaliaNeural')",
                example = "es-MX-JorgeNeural",
                defaultValue = "es-MX-JorgeNeural")
        String ttsVoice
) {
    public VideoGenerationRequest {
        if (format == null) {
            format = VideoFormat.VERTICAL_9_16;
        }
        if (withBgm == null) {
            withBgm = true;
        }
        if (withTts == null) {
            withTts = false;
        }
        if (ttsVoice == null || ttsVoice.isBlank()) {
            ttsVoice = "es-MX-JorgeNeural";
        }
    }

    public VideoGenerationRequest(List<UUID> triviaIds, VideoFormat format, Boolean withBgm) {
        this(triviaIds, format, withBgm, false, "es-MX-JorgeNeural");
    }

    public VideoGenerationRequest(List<UUID> triviaIds) {
        this(triviaIds, VideoFormat.VERTICAL_9_16, true, false, "es-MX-JorgeNeural");
    }
}
