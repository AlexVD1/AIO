package com.trivia.api.service.video;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Modalidades disponibles para la generación de la introducción del video de trivia.
 */
@Schema(description = "Modalidad de generación para la escena de introducción del video")
public enum VideoIntroMode {

    @Schema(description = "Sin escena de introducción (el video inicia directamente con la pregunta 1)")
    NONE,

    @Schema(description = "Utilizar una plantilla predefinida del catálogo sustituyendo {tema}")
    TEMPLATE,

    @Schema(description = "Utilizar un texto de introducción personalizado provisto por el usuario")
    CUSTOM,

    @Schema(description = "Generar una introducción dinámica y atractiva mediante Inteligencia Artificial (Gemini)")
    AI
}
