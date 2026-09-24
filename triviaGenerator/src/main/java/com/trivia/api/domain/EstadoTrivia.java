package com.trivia.api.domain;

/**
 * Estado de una trivia individual almacenada.
 *
 * Principio de INMUTABILIDAD:
 *   Una trivia no se elimina ni se modifica silenciosamente.
 *   Si se detecta un error, se cambia su estado y se guarda el motivo.
 *   El historial siempre queda completo para trazabilidad.
 *
 * Este enum se almacena como STRING en la base de datos (VARCHAR 15).
 * El valor más largo es "ARCHIVADA" = 9 caracteres.
 */
public enum EstadoTrivia {

    /** Trivia válida, disponible para consultas. Estado inicial. */
    ACTIVA,

    /**
     * Trivia marcada como inválida después de ser almacenada.
     * No se elimina; se conserva el registro histórico.
     * Se debe registrar el motivo de invalidación.
     */
    INVALIDA,

    /**
     * Trivia archivada por decisión administrativa o por haber sido
     * reemplazada por una versión mejor.
     * Se conserva el historial completo.
     */
    ARCHIVADA
}
