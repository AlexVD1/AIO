package com.trivia.api.domain;

/**
 * Tipo de asset (imagen) generado por trivia.
 *
 * Cada trivia produce exactamente DOS imágenes PNG:
 *   - PREGUNTA: Muestra la pregunta y las opciones SIN revelar la respuesta correcta.
 *   - RESPUESTA: Mismo layout, pero con la respuesta correcta destacada.
 *
 * Se almacena como STRING en la base de datos (VARCHAR 15).
 * El valor más largo es "RESPUESTA" = 9 caracteres.
 */
public enum TipoAsset {

    /**
     * Imagen de la pregunta.
     * Muestra el enunciado y todas las opciones con letras (A, B, C, D...).
     * La respuesta correcta NO está marcada.
     * Útil para presentar la trivia al jugador antes de responder.
     */
    PREGUNTA,

    /**
     * Imagen de la respuesta.
     * Mismo layout que PREGUNTA, pero:
     *   - Respuesta correcta: destacada con color distintivo y símbolo de check.
     *   - Respuestas incorrectas: grises, visualmente desactivadas.
     * Útil para mostrar el resultado después de que el jugador respondió.
     */
    RESPUESTA
}
