package com.trivia.api.controller;

import com.trivia.api.dto.VideoGenerationRequest;
import com.trivia.api.dto.VideoGenerationResponse;
import com.trivia.api.dto.VideoIntroPreviewRequest;
import com.trivia.api.dto.VideoIntroPreviewResponse;
import com.trivia.api.service.AssetStorageService;
import com.trivia.api.service.video.TriviaVideoService;
import com.trivia.api.service.video.VideoIntroTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la generación y descarga de videos compilatorios de trivias,
 * incluyendo configuración de introducciones temáticas dinámicas.
 */
@RestController
@RequestMapping("/api/v1/videos")
@Tag(name = "Videos", description = "Operaciones de compilación y renderizado de video para trivias")
public class TriviaVideoController {

    private final TriviaVideoService videoService;
    private final AssetStorageService storageService;

    public TriviaVideoController(TriviaVideoService videoService, AssetStorageService storageService) {
        this.videoService = videoService;
        this.storageService = storageService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Genera un video MP4 a partir de un conjunto de trivias existentes con animaciones, efectos de sonido e introducción opcional")
    public ResponseEntity<VideoGenerationResponse> generarVideo(@Valid @RequestBody VideoGenerationRequest request) {
        VideoGenerationResponse response = videoService.generarVideo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/intro-templates")
    @Operation(summary = "Obtiene el catálogo de plantillas reutilizables para la introducción del video")
    public ResponseEntity<List<VideoIntroTemplate>> getIntroTemplates() {
        return ResponseEntity.ok(videoService.getIntroTemplates());
    }

    @PostMapping("/preview-intro")
    @Operation(summary = "Previsualiza o genera el texto de introducción y detecta el tema según la modalidad seleccionada")
    public ResponseEntity<VideoIntroPreviewResponse> previewIntro(@Valid @RequestBody VideoIntroPreviewRequest request) {
        return ResponseEntity.ok(videoService.previewIntro(request));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Descarga directa del archivo MP4 de un video previamente generado")
    public void descargarVideo(
            @PathVariable UUID id,
            @RequestParam(required = false) String filename,
            HttpServletResponse response) throws IOException {
        String relativePath = "videos/" + id + ".mp4";
        if (!storageService.exists(relativePath)) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Video no encontrado para el ID: " + id);
            return;
        }

        byte[] videoBytes = storageService.retrieve(relativePath);

        String effectiveFilename = (filename != null && !filename.isBlank())
                ? videoService.sanitizeFilename(filename)
                : "trivia_video_" + id;
        if (!effectiveFilename.toLowerCase().endsWith(".mp4")) {
            effectiveFilename += ".mp4";
        }

        response.setContentType("video/mp4");
        response.setContentLength(videoBytes.length);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + effectiveFilename + "\"");

        response.getOutputStream().write(videoBytes);
        response.getOutputStream().flush();
    }
}
