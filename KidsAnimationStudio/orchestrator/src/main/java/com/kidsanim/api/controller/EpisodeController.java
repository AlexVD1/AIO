package com.kidsanim.api.controller;

import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.dto.CreateEpisodeRequest;
import com.kidsanim.api.dto.EpisodeDetailResponse;
import com.kidsanim.api.dto.EpisodeResponse;
import com.kidsanim.api.script.EpisodeScriptService;
import com.kidsanim.api.script.model.EpisodeScript;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Episodios y Guiones", description = "Planificación, generación con LLM y aprobación de guiones educativos")
public class EpisodeController {

    private final EpisodeScriptService episodeScriptService;
    private final com.kidsanim.api.service.NarrationStageService narrationStageService;
    private final com.kidsanim.api.service.KeyframeStageService keyframeStageService;
    private final com.kidsanim.api.service.AnimationStageService animationStageService;
    private final com.kidsanim.api.service.RenderStageService renderStageService;
    private final com.kidsanim.api.repository.EpisodeRepository episodeRepository;
    private final com.kidsanim.api.pipeline.EpisodePipelineExecutor episodePipelineExecutor;
    private final com.kidsanim.api.export.service.ExportService exportService;

    public EpisodeController(EpisodeScriptService episodeScriptService,
                             com.kidsanim.api.service.NarrationStageService narrationStageService,
                             com.kidsanim.api.service.KeyframeStageService keyframeStageService,
                             com.kidsanim.api.service.AnimationStageService animationStageService,
                             com.kidsanim.api.service.RenderStageService renderStageService,
                             com.kidsanim.api.repository.EpisodeRepository episodeRepository,
                             com.kidsanim.api.pipeline.EpisodePipelineExecutor episodePipelineExecutor,
                             com.kidsanim.api.export.service.ExportService exportService) {
        this.episodeScriptService = episodeScriptService;
        this.narrationStageService = narrationStageService;
        this.keyframeStageService = keyframeStageService;
        this.animationStageService = animationStageService;
        this.renderStageService = renderStageService;
        this.episodeRepository = episodeRepository;
        this.episodePipelineExecutor = episodePipelineExecutor;
        this.exportService = exportService;
    }

    @PostMapping("/series/{seriesId}/episodes")
    @Operation(summary = "Generar y planificar un nuevo episodio con guion pedagógico")
    public ResponseEntity<EpisodeResponse> createEpisode(
            @PathVariable UUID seriesId,
            @Valid @RequestBody CreateEpisodeRequest request) {
        Episode episode = episodeScriptService.generateAndSaveEpisode(seriesId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EpisodeResponse.fromEntity(episode));
    }

    @GetMapping("/series/{seriesId}/episodes")
    @Operation(summary = "Listar todos los episodios de una serie")
    public ResponseEntity<List<EpisodeResponse>> getEpisodesBySeries(@PathVariable UUID seriesId) {
        List<EpisodeResponse> episodes = episodeScriptService.findBySeriesId(seriesId)
                .stream()
                .map(EpisodeResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(episodes);
    }

    @GetMapping("/episodes/{id}")
    @Operation(summary = "Obtener el detalle de un episodio con su guion completo")
    public ResponseEntity<EpisodeDetailResponse> getEpisodeDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(episodeScriptService.getDetail(id));
    }

    @GetMapping("/episodes/{id}/script")
    @Operation(summary = "Obtener el guion pedagógico JSON estructurado del episodio")
    public ResponseEntity<EpisodeScript> getEpisodeScript(@PathVariable UUID id) {
        return ResponseEntity.ok(episodeScriptService.getScript(id));
    }

    @PostMapping("/episodes/{id}/approve-script")
    @Operation(summary = "Aprobar manualmente el guion de un episodio (avanzando a locución)")
    public ResponseEntity<EpisodeResponse> approveScript(@PathVariable UUID id) {
        Episode approved = episodeScriptService.approveScript(id);
        return ResponseEntity.ok(EpisodeResponse.fromEntity(approved));
    }

    @PostMapping("/episodes/{id}/narration")
    @Operation(summary = "Ejecutar la etapa NARRATION para sintetizar audio y alinear timestamps de todos los planos")
    public ResponseEntity<EpisodeResponse> runNarration(@PathVariable UUID id) {
        Episode narrated = narrationStageService.processEpisodeNarration(id);
        return ResponseEntity.ok(EpisodeResponse.fromEntity(narrated));
    }

    @GetMapping(value = "/episodes/{id}/narration/reading-script", produces = "text/plain;charset=UTF-8")
    @Operation(summary = "Obtener el guion de lectura en texto plano con pausas para grabación de voz humana")
    public ResponseEntity<String> getReadingScript(@PathVariable UUID id) {
        String script = narrationStageService.generateReadingScript(id);
        return ResponseEntity.ok(script);
    }

    @PostMapping(value = "/episodes/{id}/narration/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir archivo de audio grabado por el usuario para alineación y corte automático con Whisper")
    public ResponseEntity<EpisodeResponse> uploadRecordedNarration(
            @PathVariable UUID id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            Episode narrated = narrationStageService.processRecordedAudioUpload(id, file.getInputStream(), file.getOriginalFilename());
            return ResponseEntity.ok(EpisodeResponse.fromEntity(narrated));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error al leer el archivo de audio subido: " + e.getMessage(), e);
        }
    }

    @GetMapping("/shots/{shotId}/narration")
    @Operation(summary = "Obtener el audio y timestamps alineados a nivel de palabra para un plano específico")
    public ResponseEntity<com.kidsanim.api.dto.ShotNarrationResponse> getShotNarration(@PathVariable UUID shotId) {
        return ResponseEntity.ok(narrationStageService.getShotNarration(shotId));
    }

    @PostMapping("/shots/{shotId}/regenerate-narration")
    @Operation(summary = "Regenerar la locación y timestamps de un plano puntual")
    public ResponseEntity<com.kidsanim.api.dto.ShotNarrationResponse> regenerateShotNarration(@PathVariable UUID shotId) {
        narrationStageService.regenerateShotNarration(shotId);
        return ResponseEntity.ok(narrationStageService.getShotNarration(shotId));
    }

    @PostMapping("/episodes/{id}/keyframes")
    @Operation(summary = "Ejecutar la etapa KEYFRAMES para generar keyframes coherentes con IP-Adapter y QA de similitud")
    public ResponseEntity<EpisodeResponse> runKeyframes(@PathVariable UUID id) {
        Episode keyframed = keyframeStageService.processEpisodeKeyframes(id);
        return ResponseEntity.ok(EpisodeResponse.fromEntity(keyframed));
    }

    @GetMapping("/shots/{shotId}/keyframe")
    @Operation(summary = "Obtener el keyframe y metadata de QA para un plano específico")
    public ResponseEntity<com.kidsanim.api.dto.ShotKeyframeResponse> getShotKeyframe(@PathVariable UUID shotId) {
        return ResponseEntity.ok(keyframeStageService.getShotKeyframe(shotId));
    }

    @PostMapping("/shots/{shotId}/regenerate-keyframe")
    @Operation(summary = "Regenerar el keyframe de un plano puntual")
    public ResponseEntity<com.kidsanim.api.dto.ShotKeyframeResponse> regenerateShotKeyframe(@PathVariable UUID shotId) {
        keyframeStageService.regenerateShotKeyframe(shotId);
        return ResponseEntity.ok(keyframeStageService.getShotKeyframe(shotId));
    }

    @PostMapping("/episodes/{id}/animation")
    @Operation(summary = "Ejecutar la etapa ANIMATION para generar clips I2V con encadenamiento por último frame")
    public ResponseEntity<EpisodeResponse> runAnimation(@PathVariable UUID id) {
        Episode animated = animationStageService.processEpisodeAnimation(id);
        return ResponseEntity.ok(EpisodeResponse.fromEntity(animated));
    }

    @GetMapping("/shots/{shotId}/animation")
    @Operation(summary = "Obtener el clip de video y frame final generado para un plano específico")
    public ResponseEntity<com.kidsanim.api.dto.ShotAnimationResponse> getShotAnimation(@PathVariable UUID shotId) {
        return ResponseEntity.ok(animationStageService.getShotAnimation(shotId));
    }

    @PostMapping("/shots/{shotId}/regenerate-animation")
    @Operation(summary = "Regenerar la animación de un plano puntual")
    public ResponseEntity<com.kidsanim.api.dto.ShotAnimationResponse> regenerateShotAnimation(@PathVariable UUID shotId) {
        animationStageService.regenerateShotAnimation(shotId);
        return ResponseEntity.ok(animationStageService.getShotAnimation(shotId));
    }

    @PostMapping("/episodes/{id}/render")
    @Operation(summary = "Ejecutar las etapas de subtítulos karaoke, overlays pedagógicos y renderizado FFmpeg")
    public ResponseEntity<EpisodeResponse> runRender(@PathVariable UUID id) {
        Episode rendered = renderStageService.renderEpisode(id);
        return ResponseEntity.ok(EpisodeResponse.fromEntity(rendered));
    }

    @GetMapping("/episodes/{id}/video")
    @Operation(summary = "Consultar el estado, ruta y tamaño del video final del episodio")
    public ResponseEntity<com.kidsanim.api.dto.EpisodeVideoResponse> getEpisodeVideo(@PathVariable UUID id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new com.kidsanim.api.infrastructure.exception.ResourceNotFoundException("Episodio no encontrado con ID: " + id));

        String videoPath = episode.getFinalVideoPath();
        boolean exists = videoPath != null && java.nio.file.Files.exists(java.nio.file.Path.of(videoPath));
        long sizeBytes = 0;
        if (exists) {
            try {
                sizeBytes = java.nio.file.Files.size(java.nio.file.Path.of(videoPath));
            } catch (Exception ignored) {}
        }

        Double duration = episode.getDurationSeconds() != null ? episode.getDurationSeconds().doubleValue() : null;

        com.kidsanim.api.dto.EpisodeVideoResponse response = new com.kidsanim.api.dto.EpisodeVideoResponse(
                episode.getId(),
                episode.getTitle(),
                episode.getStatus().name(),
                videoPath,
                duration,
                exists,
                sizeBytes
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/episodes/{id}/start-pipeline")
    @Operation(summary = "Iniciar la ejecución asíncrona completa del pipeline para un episodio")
    public ResponseEntity<com.kidsanim.api.dto.EpisodePipelineStatusResponse> startPipeline(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean autoApprove) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new com.kidsanim.api.infrastructure.exception.ResourceNotFoundException("Episodio no encontrado con ID: " + id));

        UUID trackingId = episodePipelineExecutor.registerPipeline(episode.getId(), episode.getTitle());
        episodePipelineExecutor.executePipelineAsync(trackingId, episode.getId(), autoApprove);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(episodePipelineExecutor.getStatus(trackingId));
    }

    @PostMapping("/episodes/{id}/resume")
    @Operation(summary = "Reanudar un episodio fallido o pausado desde la etapa incompleta")
    public ResponseEntity<com.kidsanim.api.dto.EpisodePipelineStatusResponse> resumeEpisode(@PathVariable UUID id) {
        com.kidsanim.api.dto.EpisodePipelineStatusResponse response = episodePipelineExecutor.resumeEpisode(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/episodes/{id}/status")
    @Operation(summary = "Consultar el estado en vivo y progreso del pipeline de un episodio")
    public ResponseEntity<com.kidsanim.api.dto.EpisodePipelineStatusResponse> getPipelineStatus(@PathVariable UUID id) {
        com.kidsanim.api.dto.EpisodePipelineStatusResponse status = episodePipelineExecutor.getStatus(id);
        if (status == null) {
            throw new com.kidsanim.api.infrastructure.exception.ResourceNotFoundException("No se encontró estado de pipeline para episodio: " + id);
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/episodes/{id}/shots")
    @Operation(summary = "Listar todos los planos persistidos del episodio con su estado y parámetros")
    public ResponseEntity<List<com.kidsanim.api.dto.ShotDetailResponse>> getEpisodeShots(@PathVariable UUID id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new com.kidsanim.api.infrastructure.exception.ResourceNotFoundException("Episodio no encontrado con ID: " + id));
        List<com.kidsanim.api.dto.ShotDetailResponse> shots = episode.getScenes().stream()
                .flatMap(s -> s.getShots().stream())
                .sorted(java.util.Comparator.comparingInt(com.kidsanim.api.domain.Shot::getOrderIndex))
                .map(com.kidsanim.api.dto.ShotDetailResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(shots);
    }

    @PostMapping("/shots/{shotId}/regenerate")
    @Operation(summary = "Regenerar un plano puntual según la etapa solicitada (KEYFRAME, ANIMATION, NARRATION)")
    public ResponseEntity<com.kidsanim.api.dto.ShotDetailResponse> regenerateShotGeneric(
            @PathVariable UUID shotId,
            @RequestParam(defaultValue = "KEYFRAME") String stage) {
        com.kidsanim.api.domain.Shot regenerated = episodePipelineExecutor.regenerateShot(shotId, stage);
        return ResponseEntity.ok(com.kidsanim.api.dto.ShotDetailResponse.fromEntity(regenerated));
    }

    @PostMapping("/episodes/{id}/export")
    @Operation(summary = "Exportar el video final del episodio, portada y metadata para YouTube Kids")
    public ResponseEntity<com.kidsanim.api.export.dto.ExportResult> exportEpisode(@PathVariable UUID id) {
        com.kidsanim.api.export.dto.ExportResult result = exportService.exportEpisode(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/episodes/{id}/video/stream")
    @Operation(summary = "Reproducir o transmitir el video final MP4 directamente en el navegador")
    public ResponseEntity<org.springframework.core.io.Resource> streamVideo(@PathVariable UUID id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new com.kidsanim.api.infrastructure.exception.ResourceNotFoundException("Episodio no encontrado con ID: " + id));

        String videoPath = episode.getFinalVideoPath();
        if (videoPath == null || !java.nio.file.Files.exists(java.nio.file.Path.of(videoPath))) {
            throw new com.kidsanim.api.infrastructure.exception.ResourceNotFoundException("El archivo de video final no existe");
        }

        org.springframework.core.io.FileSystemResource resource = new org.springframework.core.io.FileSystemResource(videoPath);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(org.springframework.http.MediaType.valueOf("video/mp4"))
                .body(resource);
    }
}
