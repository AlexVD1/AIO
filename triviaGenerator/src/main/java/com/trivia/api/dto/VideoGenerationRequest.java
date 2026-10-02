package com.trivia.api.dto;

import com.trivia.api.service.video.VideoFormat;
import com.trivia.api.service.video.VideoIntroMode;
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
        String ttsVoice,

        @Schema(description = "Modalidad de introducción del video (NONE, TEMPLATE, CUSTOM, AI)",
                example = "TEMPLATE",
                defaultValue = "NONE")
        VideoIntroMode introMode,

        @Schema(description = "Plantilla a utilizar cuando introMode es TEMPLATE (ej. 'Pon a prueba tus conocimientos sobre {tema}.')",
                example = "¿Qué tanto sabes de {tema}?")
        String introTemplate,

        @Schema(description = "Texto personalizado a utilizar cuando introMode es CUSTOM",
                example = "¿Eres realmente un experto en Interstellar?")
        String customIntroText,

        @Schema(description = "Tema o subtema explícito para la introducción (si se omite se infiere de las trivias)",
                example = "Historia romana")
        String introTopic,

        @Schema(description = "Título personalizado para el video y publicaciones en redes sociales (si se omite se infiere de la intro o tema)",
                example = "3 Preguntas Capciosas Imposibles")
        String customTitle
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
        if (introMode == null) {
            introMode = VideoIntroMode.NONE;
        }
    }

    public VideoGenerationRequest(List<UUID> triviaIds, VideoFormat format, Boolean withBgm, Boolean withTts, String ttsVoice, VideoIntroMode introMode, String introTemplate, String customIntroText, String introTopic) {
        this(triviaIds, format, withBgm, withTts, ttsVoice, introMode, introTemplate, customIntroText, introTopic, null);
    }

    public VideoGenerationRequest(List<UUID> triviaIds, VideoFormat format, Boolean withBgm, Boolean withTts, String ttsVoice) {
        this(triviaIds, format, withBgm, withTts, ttsVoice, VideoIntroMode.NONE, null, null, null, null);
    }

    public VideoGenerationRequest(List<UUID> triviaIds, VideoFormat format, Boolean withBgm) {
        this(triviaIds, format, withBgm, false, "es-MX-JorgeNeural", VideoIntroMode.NONE, null, null, null, null);
    }

    public VideoGenerationRequest(List<UUID> triviaIds) {
        this(triviaIds, VideoFormat.VERTICAL_9_16, true, false, "es-MX-JorgeNeural", VideoIntroMode.NONE, null, null, null, null);
    }
}
