package com.trivia.api.domain;

/**
 * Estado de una trivia individual almacenada.
 *
 * Principio de INMUTABILIDAD:
 *   Una trivia no se elimina ni se modifica silenciosamente.
 *   Si se detecta un error, se cambia su estado y se guarda el motivo.
 *   El historial siempre queda completo para trazabilidad.
 *
 * Este enum se almacena como STRING en la base de datos (VARCHAR 35).
 */
public enum EstadoTrivia {

    /** Trivia válida, recién creada y disponible para consultas. Estado inicial. */
    ACTIVA,

    /**
     * Trivia que ha sido descargada exitosamente en paquete ZIP (JSON + imágenes),
     * pero aún no se le ha generado video compilatorio.
     */
    DESCARGADA,

    /**
     * Trivia para la cual ya se compiló/exportó exitosamente un video MP4,
     * pero aún no ha sido descargada en paquete ZIP.
     */
    EXPORTADA,

    /**
     * Trivia completa: ha sido descargada en paquete ZIP Y además ya se generó
     * su video compilatorio MP4.
     */
    DESCARGADA_Y_EXPORTADA,

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
