package com.trivia.api.domain;

/**
 * Nivel de dificultad de una trivia.
 *
 * Se almacena como STRING en la base de datos (VARCHAR 10).
 * El valor más largo es "DIFICIL" = 7 caracteres.
 *
 * Nota: sin tilde en DIFICIL para mantener consistencia
 * como identificador técnico y evitar problemas de encoding.
 */
public enum Dificultad {

    /** Trivia sencilla, conocimiento básico. */
    FACIL,

    /** Trivia de dificultad intermedia. */
    MEDIA,

    /** Trivia que requiere conocimiento profundo o específico. */
    DIFICIL
}
