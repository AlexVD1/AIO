package com.storyvideo.api.controller;

import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.dto.StorySceneResponseDto;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.service.StoryGenerationService;
import com.storyvideo.api.service.StoryVisualService;
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

import com.storyvideo.api.export.dto.ExportMetadata;
import com.storyvideo.api.export.dto.ExportResult;
import com.storyvideo.api.export.service.ExportService;
import com.storyvideo.api.pipeline.dto.PipelineStatusDto;
import com.storyvideo.api.pipeline.service.StoryPipelineExecutor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stories")
@Tag(name = "Stories", description = "Generación y consulta de historias estructuradas para videos")
public class StoryController {

    private final StoryGenerationService storyGenerationService;
    private final StoryVisualService storyVisualService;
    private final com.storyvideo.api.service.StoryAudioService storyAudioService;
    private final com.storyvideo.api.service.VideoRenderService videoRenderService;
    private final StoryRepository storyRepository;
    private final AssetStorageService storageService;
    private final ExportService exportService;
    private final StoryPipelineExecutor storyPipelineExecutor;

    public StoryController(
            StoryGenerationService storyGenerationService,
            StoryVisualService storyVisualService,
            com.storyvideo.api.service.StoryAudioService storyAudioService,
            com.storyvideo.api.service.VideoRenderService videoRenderService,
            StoryRepository storyRepository,
            AssetStorageService storageService,
            ExportService exportService,
            StoryPipelineExecutor storyPipelineExecutor) {
        this.storyGenerationService = storyGenerationService;
        this.storyVisualService = storyVisualService;
        this.storyAudioService = storyAudioService;
        this.videoRenderService = videoRenderService;
        this.storyRepository = storyRepository;
        this.storageService = storageService;
        this.exportService = exportService;
        this.storyPipelineExecutor = storyPipelineExecutor;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generar historia estructurada", description = "Crea una nueva historia completa con hook, personajes y desglose de escenas usando IA")
    public ResponseEntity<StoryResponseDto> generateStory(@Valid @RequestBody StoryGenerationRequestDto request) {
        StoryResponseDto response = storyGenerationService.generateAndPersistStory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar historia por ID", description = "Retorna el detalle completo de la historia, sus personajes y escenas")
    public ResponseEntity<StoryResponseDto> getStoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(storyGenerationService.getStoryById(id));
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @Operation(summary = "Listar historias", description = "Lista paginada con filtros opcionales de género y estado")
    public ResponseEntity<Page<StoryResponseDto>> listStories(
            @RequestParam(required = false) StoryGenre genre,
            @RequestParam(required = false) StoryStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<StoryResponseDto> page;
        if (genre != null && status != null) {
            page = storyRepository.findByGenreAndStatus(genre, status, pageable)
                    .map(s -> StoryResponseDto.fromEntity(s, storageService));
        } else if (genre != null) {
            page = storyRepository.findByGenre(genre, pageable)
                    .map(s -> StoryResponseDto.fromEntity(s, storageService));
        } else if (status != null) {
            page = storyRepository.findByStatus(status, pageable)
                    .map(s -> StoryResponseDto.fromEntity(s, storageService));
        } else {
            page = storyRepository.findAll(pageable)
                    .map(s -> StoryResponseDto.fromEntity(s, storageService));
        }

        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/generate-images")
    @Operation(summary = "Generar imágenes para la historia", description = "Genera las imágenes para todas las escenas pendientes de la historia usando Stable Diffusion local o fallback")
    public ResponseEntity<StoryResponseDto> generateImages(@PathVariable UUID id) {
        StoryResponseDto response = storyVisualService.generateImagesForStory(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/scenes/{sceneId}/regenerate-image")
    @Operation(summary = "Regenerar imagen de una escena", description = "Vuelve a generar la imagen para una escena específica con una nueva semilla sin modificar el resto de la historia")
    public ResponseEntity<StorySceneResponseDto> regenerateSceneImage(
            @PathVariable UUID id,
            @PathVariable UUID sceneId) {
        StorySceneResponseDto response = storyVisualService.regenerateSceneImage(id, sceneId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate-audio")
    @Operation(summary = "Generar locución para la historia", description = "Sintetiza la narración de todas las escenas con Edge-TTS y recalcula el timeline acumulativo")
    public ResponseEntity<StoryResponseDto> generateAudio(
            @PathVariable UUID id,
            @RequestParam(required = false) String voice) {
        StoryResponseDto response = storyAudioService.generateAudioForStory(id, voice);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/scenes/{sceneId}/regenerate-audio")
    @Operation(summary = "Regenerar locución de una escena", description = "Vuelve a sintetizar la narración de una escena y recalcula los offsets temporales subsiguientes")
    public ResponseEntity<StorySceneResponseDto> regenerateSceneAudio(
            @PathVariable UUID id,
            @PathVariable UUID sceneId,
            @RequestParam(required = false) String voice) {
        StorySceneResponseDto response = storyAudioService.regenerateSceneAudio(id, sceneId, voice);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/render")
    @Operation(summary = "Renderizar video final", description = "Construye el timeline, compone el filtergraph de video y audio con FFmpeg, renderiza a MP4 vertical (1080x1920) y valida la calidad")
    public ResponseEntity<com.storyvideo.api.dto.VideoRenderResponseDto> renderVideo(@PathVariable UUID id) {
        com.storyvideo.api.dto.VideoRenderResponseDto response = videoRenderService.renderVideo(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/render")
    @Operation(summary = "Consultar último render de la historia", description = "Obtiene los detalles del último render ejecutado para la historia, incluyendo enlace público y resultado de validación")
    public ResponseEntity<com.storyvideo.api.dto.VideoRenderResponseDto> getLatestRender(@PathVariable UUID id) {
        com.storyvideo.api.dto.VideoRenderResponseDto response = videoRenderService.getLatestRender(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/video/download")
    @Operation(summary = "Descargar MP4 final", description = "Descarga el archivo de video vertical MP4 terminado para la historia")
    public ResponseEntity<Resource> downloadVideo(@PathVariable UUID id) {
        Path videoPath = exportService.getExportedVideoPath(id);
        Resource resource = new FileSystemResource(videoPath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + videoPath.getFileName().toString() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/video/metadata")
    @Operation(summary = "Consultar metadata de exportación", description = "Obtiene los metadatos completos para publicación en Make.com o redes sociales")
    public ResponseEntity<ExportMetadata> getExportMetadata(@PathVariable UUID id) {
        ExportMetadata metadata = exportService.getExportMetadata(id);
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/{id}/export")
    @Operation(summary = "Exportar video y generar metadata", description = "Copia el MP4 final al directorio de exportación y genera el archivo metadata.json")
    public ResponseEntity<ExportResult> exportStory(@PathVariable UUID id) {
        ExportResult result = exportService.exportStoryVideo(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/rerender")
    @Operation(summary = "Re-renderizar video", description = "Vuelve a componer y renderizar el video con FFmpeg sin regenerar las imágenes ni la locución")
    public ResponseEntity<com.storyvideo.api.dto.VideoRenderResponseDto> rerenderVideo(@PathVariable UUID id) {
        com.storyvideo.api.dto.VideoRenderResponseDto response = videoRenderService.renderVideo(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Reintentar historia fallida", description = "Reejecuta el pipeline para una historia que haya terminado en estado FAILED")
    public ResponseEntity<PipelineStatusDto> retryStory(@PathVariable UUID id) {
        UUID trackingId = UUID.randomUUID();
        PipelineStatusDto status = storyPipelineExecutor.executeExistingStory(trackingId, id);
        return ResponseEntity.ok(status);
    }
}
