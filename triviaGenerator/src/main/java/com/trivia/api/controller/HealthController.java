package com.trivia.api.controller;

import com.trivia.api.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de Health Check.
 *
 * GET /api/v1/health — Verifica que la API está activa.
 *
 * Este endpoint es público (sin API Key) porque:
 * - Los healthchecks de Podman lo usan antes de autenticar
 * - Los balanceadores de carga/proxies también lo requieren sin auth
 *
 * La anotación @Tag asocia este controller al grupo "Health" en Swagger UI.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Estado de la API")
public class HealthController {

    /**
     * Retorna HTTP 200 con información básica del servicio.
     *
     * ResponseEntity<T> nos permite controlar explícitamente:
     * - El código HTTP de respuesta
     * - Los headers de respuesta
     * - El cuerpo de respuesta
     *
     * En este caso, siempre devuelve 200 OK si la JVM está viva.
     * En Fase 3, agregaremos verificación del estado de la DB.
     */
    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Verifica que la API está activa y funcionando"
    )
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.up());
    }
}
