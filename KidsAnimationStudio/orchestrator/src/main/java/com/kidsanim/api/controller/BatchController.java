package com.kidsanim.api.controller;

import com.kidsanim.api.dto.BatchResponse;
import com.kidsanim.api.dto.CreateBatchRequest;
import com.kidsanim.api.service.BatchJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batches")
@Tag(name = "Producción en Lote", description = "Generación masiva y secuencial de episodios educativos para una serie")
public class BatchController {

    private final BatchJobService batchJobService;

    public BatchController(BatchJobService batchJobService) {
        this.batchJobService = batchJobService;
    }

    @PostMapping
    @Operation(summary = "Crear e iniciar un lote de producción secuencial de episodios")
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        BatchResponse response = batchJobService.createBatch(request);
        // Disparar procesamiento asíncrono en cola de concurrencia 1
        batchJobService.executeBatchAsync(response.id(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar el estado y progreso de un lote de producción")
    public ResponseEntity<BatchResponse> getBatch(@PathVariable UUID id) {
        return ResponseEntity.ok(batchJobService.getBatch(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar la producción de episodios pendientes en un lote")
    public ResponseEntity<BatchResponse> cancelBatch(@PathVariable UUID id) {
        return ResponseEntity.ok(batchJobService.cancelBatch(id));
    }
}
