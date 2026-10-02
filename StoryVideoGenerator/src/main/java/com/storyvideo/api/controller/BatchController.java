package com.storyvideo.api.controller;

import com.storyvideo.api.batch.dto.BatchGenerationRequestDto;
import com.storyvideo.api.batch.dto.BatchStatusResponseDto;
import com.storyvideo.api.batch.service.BatchProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@Tag(name = "Batch", description = "Generación y gestión de lotes masivos de videos")
public class BatchController {

    private final BatchProcessingService batchProcessingService;

    public BatchController(BatchProcessingService batchProcessingService) {
        this.batchProcessingService = batchProcessingService;
    }

    @PostMapping("/api/v1/stories/batch")
    @Operation(
            summary = "Generar lote masivo de historias",
            description = "Encola la creación de múltiples historias por género con concurrencia controlada, aislamiento de errores y seguimiento de estado"
    )
    public ResponseEntity<BatchStatusResponseDto> createBatch(@Valid @RequestBody BatchGenerationRequestDto request) {
        BatchStatusResponseDto response = batchProcessingService.submitBatch(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/v1/batches/" + response.batchId() + "/status"))
                .body(response);
    }

    @GetMapping({"/api/v1/batches/{id}/status", "/api/v1/batches/{id}"})
    @Operation(
            summary = "Consultar estado del lote",
            description = "Devuelve el resumen del lote: total solicitadas, completadas, fallidas, porcentaje de avance y detalle por historia"
    )
    public ResponseEntity<BatchStatusResponseDto> getBatchStatus(@PathVariable UUID id) {
        BatchStatusResponseDto status = batchProcessingService.getBatchStatus(id);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/api/v1/batches/{id}/retry-failed")
    @Operation(
            summary = "Reintentar historias fallidas del lote",
            description = "Reejecuta de manera asíncrona únicamente las historias del lote que terminaron en estado FAILED"
    )
    public ResponseEntity<BatchStatusResponseDto> retryFailedStories(@PathVariable UUID id) {
        batchProcessingService.retryFailedStoriesAsync(id);
        BatchStatusResponseDto currentStatus = batchProcessingService.getBatchStatus(id);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/v1/batches/" + id + "/status"))
                .body(currentStatus);
    }
}
