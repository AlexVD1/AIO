package com.trivia.api.service.video;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representa una plantilla reutilizable del catálogo de introducciones de video.
 */
@Schema(description = "Plantilla reutilizable del catálogo de introducciones")
public record VideoIntroTemplate(
        @Schema(description = "Identificador único de la plantilla", example = "tpl_pon_a_prueba")
        String id,

        @Schema(description = "Patrón de texto con comodín {tema}", example = "Pon a prueba tus conocimientos sobre {tema}.")
        String template,

        @Schema(description = "Ejemplo ilustrativo de cómo queda la frase", example = "Pon a prueba tus conocimientos sobre Historia romana.")
        String example
) {}
