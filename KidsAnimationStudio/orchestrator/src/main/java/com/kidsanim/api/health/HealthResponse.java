package com.kidsanim.api.health;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Respuesta agregada de /api/v1/health.
 * status: UP (todo arriba), DEGRADED (DB arriba pero algún motor de IA caído), DOWN (DB caída).
 */
public record HealthResponse(
        String status,
        String service,
        OffsetDateTime timestamp,
        Map<String, ComponentHealth> components
) {
}
