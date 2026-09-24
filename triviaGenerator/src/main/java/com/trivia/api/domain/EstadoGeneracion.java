package com.trivia.api.domain;

/**
 * Estados posibles de una generación de trivias.
 *
 * El flujo normal es:
 *   PENDIENTE → GENERANDO → VALIDANDO → BUSCANDO_DUPLICADOS
 *   → REGENERANDO (si faltan trivias) → GUARDANDO → RENDERIZANDO → COMPLETADA
 *
 * En cualquier punto puede ocurrir ERROR.
 *
 * Este enum se almacena como STRING en la base de datos (VARCHAR 30).
 * La restricción de tamaño la impone la columna, no Java.
 * El valor más largo es "BUSCANDO_DUPLICADOS" = 18 caracteres.
 */
public enum EstadoGeneracion {

    /** Generación creada, aún no iniciada. Estado inicial. */
    PENDIENTE,

    /** Llamando al LLM para generar preguntas. */
    GENERANDO,

    /** Validando estructura y contenido de las preguntas recibidas. */
    VALIDANDO,

    /** Comparando preguntas contra la base de datos para detectar duplicados. */
    BUSCANDO_DUPLICADOS,

    /** No se consiguió la cantidad solicitada; solicitando más preguntas al LLM. */
    REGENERANDO,

    /** Persistiendo trivias válidas en la base de datos. */
    GUARDANDO,

    /** Generando imágenes PNG para cada trivia. */
    RENDERIZANDO,

    /** Generación exitosa. Todas las trivias están almacenadas. Estado terminal. */
    COMPLETADA,

    /** Ocurrió un error no recuperable. Estado terminal. */
    ERROR
}
