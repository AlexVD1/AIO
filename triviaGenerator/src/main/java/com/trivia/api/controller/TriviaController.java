package com.trivia.api.controller;

import com.trivia.api.dto.StatsResponse;
import com.trivia.api.dto.TriviaGenerationRequest;
import com.trivia.api.dto.TriviaGenerationResponse;
import com.trivia.api.dto.TriviaResponse;
import com.trivia.api.service.TriviaGenerationService;
import com.trivia.api.service.TriviaQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador principal de la API de trivias.
 *
 * Expone endpoints para:
 *   - Generación de contenido con IA (POST /api/v1/trivias)
 *   - Consulta por ID (GET /api/v1/trivias/{id})
 *   - Consulta de trivia aleatoria (GET /api/v1/trivias/random)
 *   - Búsqueda por texto (GET /api/v1/trivias/search)
 *   - Listado general paginado (GET /api/v1/trivias)
 *   - Métricas y estadísticas (GET /api/v1/trivias/stats)
 */
@RestController
@RequestMapping("/api/v1/trivias")
@Tag(name = "Trivias", description = "Operaciones de generación y consulta de contenido de trivia")
public class TriviaController {

    private final TriviaGenerationService generationService;
    private final TriviaQueryService queryService;
    private final com.trivia.api.service.AsyncTriviaPipelineExecutor asyncPipelineExecutor;

    public TriviaController(
            TriviaGenerationService generationService,
            TriviaQueryService queryService,
            com.trivia.api.service.AsyncTriviaPipelineExecutor asyncPipelineExecutor) {
        this.generationService = generationService;
        this.queryService = queryService;
        this.asyncPipelineExecutor = asyncPipelineExecutor;
    }

    @PostMapping
    @Operation(summary = "Genera nuevas trivias mediante IA con deduplicación automática (síncrono)")
    public ResponseEntity<TriviaGenerationResponse> generarTrivias(
            @Valid @RequestBody TriviaGenerationRequest request) {
        TriviaGenerationResponse response = generationService.generarTrivias(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/async")
    @Operation(summary = "Solicita generación asíncrona de trivias (retorna HTTP 202 Accepted para polling de estado)")
    public ResponseEntity<TriviaGenerationResponse> generarTriviasAsync(
            @Valid @RequestBody TriviaGenerationRequest request) {
        TriviaGenerationResponse pendiente = generationService.registrarSolicitudPendiente(request);
        asyncPipelineExecutor.ejecutarPipelineAsync(pendiente.id());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pendiente);
    }

    @GetMapping("/generations/{id}/status")
    @Operation(summary = "Consulta el estado en vivo de una solicitud de generación")
    public ResponseEntity<TriviaGenerationResponse> obtenerEstadoGeneracion(@PathVariable UUID id) {
        return ResponseEntity.ok(generationService.obtenerEstadoGeneracion(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una trivia por su identificador UUID")
    public ResponseEntity<TriviaResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.obtenerPorId(id));
    }

    @GetMapping("/random")
    @Operation(summary = "Obtiene una trivia activa aleatoria")
    public ResponseEntity<TriviaResponse> obtenerAleatoria() {
        return ResponseEntity.ok(queryService.obtenerAleatoria());
    }

    @GetMapping("/search")
    @Operation(summary = "Busca trivias por coincidencia de texto en la pregunta o explicación")
    public ResponseEntity<Page<TriviaResponse>> buscar(
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.buscarPorTexto(query, pageable));
    }

    @GetMapping
    @Operation(summary = "Lista todas las trivias de forma paginada")
    public ResponseEntity<Page<TriviaResponse>> listar(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listar(pageable));
    }

    @GetMapping("/stats")
    @Operation(summary = "Obtiene estadísticas globales de la plataforma")
    public ResponseEntity<StatsResponse> obtenerEstadisticas() {
        return ResponseEntity.ok(queryService.obtenerEstadisticas());
    }
}
