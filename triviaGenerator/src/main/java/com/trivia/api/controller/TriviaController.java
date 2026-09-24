package com.trivia.api.controller;

import com.trivia.api.dto.StatsResponse;
import com.trivia.api.dto.TriviaExportRequest;
import com.trivia.api.dto.TriviaGenerationRequest;
import com.trivia.api.dto.TriviaGenerationResponse;
import com.trivia.api.dto.TriviaResponse;
import com.trivia.api.service.AsyncTriviaPipelineExecutor;
import com.trivia.api.service.TriviaExportService;
import com.trivia.api.service.TriviaGenerationService;
import com.trivia.api.service.TriviaQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Controlador principal de la API de trivias.
 *
 * Expone endpoints para:
 *   - Generación de contenido con IA (POST /api/v1/trivias)
 *   - Consulta por ID (GET /api/v1/trivias/{id})
 *   - Consulta de trivia aleatoria (GET /api/v1/trivias/random)
 *   - Búsqueda/filtrado combinado (GET /api/v1/trivias/search)
 *   - Listado general paginado (GET /api/v1/trivias)
 *   - Métricas y estadísticas (GET /api/v1/trivias/stats)
 *   - Exportación de trivias e imágenes en archivo ZIP (POST /api/v1/trivias/export, GET /api/v1/trivias/{id}/export)
 *   - Marcar trivias como descargadas (PATCH /api/v1/trivias/mark-downloaded)
 */
@RestController
@RequestMapping("/api/v1/trivias")
@Tag(name = "Trivias", description = "Operaciones de generación y consulta de contenido de trivia")
public class TriviaController {

    private final TriviaGenerationService generationService;
    private final TriviaQueryService queryService;
    private final AsyncTriviaPipelineExecutor asyncPipelineExecutor;
    private final TriviaExportService exportService;

    public TriviaController(
            TriviaGenerationService generationService,
            TriviaQueryService queryService,
            AsyncTriviaPipelineExecutor asyncPipelineExecutor,
            TriviaExportService exportService) {
        this.generationService = generationService;
        this.queryService = queryService;
        this.asyncPipelineExecutor = asyncPipelineExecutor;
        this.exportService = exportService;
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
    @Operation(summary = "Busca/filtra trivias por texto libre, estado, dificultad, subtema y tipo")
    public ResponseEntity<Page<TriviaResponse>> buscar(
            @RequestParam(name = "q",           required = false) String query,
            @RequestParam(name = "estado",      required = false) String estado,
            @RequestParam(name = "dificultad",  required = false) String dificultad,
            @RequestParam(name = "subtema",     required = false) String subtema,
            @RequestParam(name = "tipoTrivia",  required = false) String tipoTrivia,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.buscarConFiltros(query, estado, dificultad, subtema, tipoTrivia, pageable));
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

    @PostMapping(value = "/export", produces = {"application/zip", "application/octet-stream", "*/*"})
    @Operation(summary = "Exporta trivias seleccionadas en un archivo ZIP con metadatos JSON e imágenes 1080x1080")
    public void exportarTrivias(
            @Valid @RequestBody TriviaExportRequest request,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        String filename = (request.triviaIds() != null && request.triviaIds().size() == 1)
                ? String.format("%s.zip", request.triviaIds().get(0))
                : "trivias_export.zip";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        exportService.exportarZip(request.triviaIds(), response.getOutputStream());
        queryService.marcarComoDescargadas(request.triviaIds());
    }

    @GetMapping(value = "/{id}/export", produces = {"application/zip", "application/octet-stream", "*/*"})
    @Operation(summary = "Exporta una trivia individual en un archivo ZIP con su JSON e imágenes")
    public void exportarTriviaIndividual(
            @PathVariable UUID id,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".zip\"");
        exportService.exportarZip(List.of(id), response.getOutputStream());
        queryService.marcarComoDescargadas(List.of(id));
    }

    @PatchMapping("/mark-downloaded")
    @Operation(summary = "Marca las trivias especificadas con estado DESCARGADA. " +
                         "Debe invocarse ÚNICAMENTE después de que el ZIP haya sido descargado exitosamente.")
    public ResponseEntity<Void> marcarDescargadas(@RequestBody TriviaExportRequest request) {
        queryService.marcarComoDescargadas(request.triviaIds());
        return ResponseEntity.noContent().build();
    }
}
