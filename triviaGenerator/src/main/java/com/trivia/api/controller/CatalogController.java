package com.trivia.api.controller;

import com.trivia.api.dto.TipoTriviaResponse;
import com.trivia.api.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints del catálogo de la plataforma.
 *
 * Los endpoints de catálogo son públicos (no requieren API Key).
 * Cualquier cliente puede consultar qué tipos de trivia están disponibles
 * antes de hacer su solicitud de generación.
 *
 * Base path: /api/v1/catalogos
 */
@RestController
@RequestMapping("/api/v1/catalogos")
@Tag(name = "Catálogo", description = "Consulta de catálogos del sistema")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Lista todos los tipos de trivia activos disponibles para generación.
     *
     * Response 200:
     * [
     *   {
     *     "id": "uuid",
     *     "codigo": "ASTRONOMIA",
     *     "nombre": "Astronomía",
     *     "descripcion": "Planetas, estrellas, galaxias..."
     *   },
     *   ...
     * ]
     *
     * Response 200 con lista vacía [] es válido (sistema recién iniciado sin catálogo).
     * Response nunca es 404 porque "tipos vacíos" es un estado válido.
     */
    @GetMapping("/tipos-trivia")
    @Operation(
            summary = "Lista tipos de trivia disponibles",
            description = "Retorna todos los tipos de trivia activos. " +
                          "Usa el campo 'codigo' para solicitar generación de trivias.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de tipos activos (puede estar vacía)",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = TipoTriviaResponse.class))
                            )
                    )
            }
    )
    public ResponseEntity<List<TipoTriviaResponse>> listarTiposTrivia() {
        return ResponseEntity.ok(catalogService.obtenerTiposActivos());
    }
}
