package com.storyvideo.api.controller;

import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.pipeline.dto.PipelineStatusDto;
import com.storyvideo.api.pipeline.service.StoryPipelineExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipeline")
@Tag(name = "Pipeline", description = "Ejecución y seguimiento asíncrono del pipeline completo de StoryVideoGenerator")
public class PipelineController {

    private final StoryPipelineExecutor storyPipelineExecutor;

    public PipelineController(StoryPipelineExecutor storyPipelineExecutor) {
        this.storyPipelineExecutor = storyPipelineExecutor;
    }

    @PostMapping("/execute")
    @Operation(
            summary = "Ejecutar pipeline completo de generación de video",
            description = "Inicia el flujo asíncrono: Idea -> Historia IA -> Imágenes -> Narración Edge-TTS -> Renderizado FFmpeg 1080x1920 -> Exportación MP4 con metadata.json"
    )
    public ResponseEntity<PipelineStatusDto> executePipeline(@Valid @RequestBody StoryGenerationRequestDto request) {
        UUID trackingId = storyPipelineExecutor.registerPipeline();
        storyPipelineExecutor.executeAsync(trackingId, request);

        PipelineStatusDto initialStatus = storyPipelineExecutor.getStatus(trackingId)
                .orElse(PipelineStatusDto.initial(trackingId));

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/v1/pipeline/status/" + trackingId))
                .body(initialStatus);
    }

    @GetMapping("/status/{id}")
    @Operation(
            summary = "Consultar estado del pipeline",
            description = "Retorna el progreso porcentual, etapa actual, mensajes y enlaces al video generado o archivo exportado"
    )
    public ResponseEntity<PipelineStatusDto> getPipelineStatus(@PathVariable UUID id) {
        return storyPipelineExecutor.getStatus(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchElementException("Pipeline no encontrado con ID o tracking: " + id));
    }
}
