package com.trivia.api.dto;

import java.time.Instant;

/**
 * Respuesta del endpoint GET /api/v1/health.
 *
 * Usamos Java Record (Java 16+) porque es un DTO puro:
 * - Inmutable por defecto
 * - Constructor, getters, equals, hashCode y toString generados automáticamente
 * - Menos código boilerplate que una clase tradicional
 *
 * @param status    Estado de la aplicación ("UP" o "DOWN")
 * @param servicio  Nombre del servicio
 * @param version   Versión del servicio
 * @param timestamp Momento en que se consultó el health (ISO 8601)
 */
public record HealthResponse(
        String status,
        String servicio,
        String version,
        String timestamp
) {

    /**
     * Factory method estático para construir una respuesta "UP" fácilmente.
     * Centraliza la lógica para que el controller sea más limpio.
     */
    public static HealthResponse up() {
        return new HealthResponse(
                "UP",
                "trivia-api",
                "0.0.1-SNAPSHOT",
                Instant.now().toString()
        );
    }
}
