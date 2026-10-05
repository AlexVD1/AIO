package com.kidsanim.api.pipeline;

import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.dto.EpisodePipelineStatusResponse;
import com.kidsanim.api.export.dto.ExportResult;
import com.kidsanim.api.export.service.ExportService;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.ShotRepository;
import com.kidsanim.api.script.EpisodeScriptService;
import com.kidsanim.api.service.AnimationStageService;
import com.kidsanim.api.service.KeyframeStageService;
import com.kidsanim.api.service.NarrationStageService;
import com.kidsanim.api.service.RenderStageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EpisodePipelineExecutor {

    private static final Logger log = LoggerFactory.getLogger(EpisodePipelineExecutor.class);

    private final EpisodeRepository episodeRepository;
    private final ShotRepository shotRepository;
    private final EpisodeScriptService episodeScriptService;
    private final NarrationStageService narrationStageService;
    private final KeyframeStageService keyframeStageService;
    private final AnimationStageService animationStageService;
    private final RenderStageService renderStageService;
    private final ExportService exportService;

    private final Map<UUID, EpisodePipelineStatusResponse> statusTracker = new ConcurrentHashMap<>();

    public EpisodePipelineExecutor(
            EpisodeRepository episodeRepository,
            ShotRepository shotRepository,
            EpisodeScriptService episodeScriptService,
            NarrationStageService narrationStageService,
            KeyframeStageService keyframeStageService,
            AnimationStageService animationStageService,
            RenderStageService renderStageService,
            ExportService exportService) {
        this.episodeRepository = episodeRepository;
        this.shotRepository = shotRepository;
        this.episodeScriptService = episodeScriptService;
        this.narrationStageService = narrationStageService;
        this.keyframeStageService = keyframeStageService;
        this.animationStageService = animationStageService;
        this.renderStageService = renderStageService;
        this.exportService = exportService;
    }

    public UUID registerPipeline(UUID episodeId, String title) {
        UUID trackingId = UUID.randomUUID();
        EpisodePipelineStatusResponse initial = EpisodePipelineStatusResponse.initial(episodeId, trackingId, title);
        statusTracker.put(trackingId, initial);
        if (episodeId != null) {
            statusTracker.put(episodeId, initial);
        }
        return trackingId;
    }

    public EpisodePipelineStatusResponse getStatus(UUID key) {
        if (key == null) return null;
        EpisodePipelineStatusResponse response = statusTracker.get(key);
        if (response != null) return response;

        // Fallback: construir desde la base de datos si existe el episodio
        return episodeRepository.findById(key)
                .map(ep -> new EpisodePipelineStatusResponse(
                        ep.getId(),
                        null,
                        ep.getTitle(),
                        ep.getStatus().name(),
                        ep.getStatus().name(),
                        calculateProgressPercent(ep.getStatus()),
                        "Estado actual en base de datos: " + ep.getStatus(),
                        ep.getFinalVideoPath(),
                        null,
                        ep.getCreatedAt(),
                        ep.getStatus() == EpisodeStatus.COMPLETED ? ep.getUpdatedAt() : null
                ))
                .orElse(null);
    }

    public EpisodePipelineStatusResponse startEpisodePipeline(UUID episodeId, boolean autoApprove) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));
        UUID trackingId = registerPipeline(episodeId, episode.getTitle());
        return executePipeline(trackingId, episodeId, autoApprove);
    }

    @Async("kidsPipelineExecutor")
    public CompletableFuture<EpisodePipelineStatusResponse> executePipelineAsync(UUID trackingId, UUID episodeId, boolean autoApprove) {
        return CompletableFuture.completedFuture(executePipeline(trackingId, episodeId, autoApprove));
    }

    public EpisodePipelineStatusResponse executePipeline(UUID trackingId, UUID episodeId, boolean autoApprove) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        log.info("Iniciando ejecución completa del pipeline para episodio '{}' ({})", episode.getTitle(), episodeId);

        try {
            // 1. Verificar estado de guion y aprobación
            if (episode.getStatus() == EpisodeStatus.DRAFT || episode.getStatus() == EpisodeStatus.PLANNING || episode.getStatus() == EpisodeStatus.SAFETY_REVIEW) {
                if (!autoApprove) {
                    episode.setStatus(EpisodeStatus.AWAITING_SCRIPT_APPROVAL);
                    episodeRepository.save(episode);
                    updateStage(trackingId, episodeId, episode.getTitle(), "AWAITING_SCRIPT_APPROVAL", 10,
                            "Guion generado y revisado. Esperando aprobación manual.", null, null, startedAt, null);
                    return getStatus(episodeId);
                }
            }

            if (episode.getStatus() == EpisodeStatus.AWAITING_SCRIPT_APPROVAL && !autoApprove) {
                updateStage(trackingId, episodeId, episode.getTitle(), "AWAITING_SCRIPT_APPROVAL", 10,
                        "El episodio está en espera de aprobación de guion.", null, null, startedAt, null);
                return getStatus(episodeId);
            }

            // 2. Etapa NARRATION
            updateStage(trackingId, episodeId, episode.getTitle(), "NARRATION", 20,
                    "Generando narración infantil con Kokoro-82M y alineando timestamps...", null, null, startedAt, null);
            episode = narrationStageService.processEpisodeNarration(episodeId);

            // 3. Etapa KEYFRAMES
            updateStage(trackingId, episodeId, episode.getTitle(), "KEYFRAMES", 40,
                    "Generando keyframes coherentes con SDXL + IP-Adapter y control de calidad...", null, null, startedAt, null);
            episode = keyframeStageService.processEpisodeKeyframes(episodeId);

            // 4. Etapa ANIMATION
            updateStage(trackingId, episodeId, episode.getTitle(), "ANIMATION", 70,
                    "Generando animación de video I2V (LTX-Video / Wan) y fallback Ken Burns...", null, null, startedAt, null);
            episode = animationStageService.processEpisodeAnimation(episodeId);

            // 5. Etapas SUBTITLES_OVERLAYS y RENDERING
            updateStage(trackingId, episodeId, episode.getTitle(), "RENDERING", 88,
                    "Renderizando video con subtítulos karaoke, overlays animados didácticos y mezcla BGM/SFX...", null, null, startedAt, null);
            RenderStageService.EpisodeRenderResult renderResult = renderStageService.renderEpisodeWithResult(episodeId);
            episode = renderResult.episode();

            // 6. Etapa EXPORTING
            updateStage(trackingId, episodeId, episode.getTitle(), "EXPORTING", 95,
                    "Exportando video final a carpeta de distribución, generando thumbnail y metadata...", null, null, startedAt, null);
            ExportResult exportResult = exportService.exportEpisode(episodeId);

            // 7. COMPLETED
            OffsetDateTime finishedAt = OffsetDateTime.now();
            updateStage(trackingId, episodeId, episode.getTitle(), "COMPLETED", 100,
                    "¡Episodio completado y exportado exitosamente!", exportResult.exportedVideoPath(), null, startedAt, finishedAt);

            log.info("Pipeline completado exitosamente para episodio '{}' ({})", episode.getTitle(), episodeId);
            return getStatus(episodeId);

        } catch (Exception e) {
            log.error("Fallo durante la ejecución del pipeline para episodio {}: {}", episodeId, e.getMessage(), e);
            try {
                episode.setStatus(EpisodeStatus.FAILED);
                episodeRepository.save(episode);
            } catch (Exception ignored) {}

            OffsetDateTime finishedAt = OffsetDateTime.now();
            EpisodePipelineStatusResponse failed = new EpisodePipelineStatusResponse(
                    episodeId,
                    trackingId,
                    episode.getTitle(),
                    "FAILED",
                    "FAILED",
                    statusTracker.getOrDefault(episodeId, EpisodePipelineStatusResponse.initial(episodeId, trackingId, episode.getTitle())).progressPercent(),
                    "Error durante el pipeline: " + e.getMessage(),
                    null,
                    e.getMessage(),
                    startedAt,
                    finishedAt
            );
            statusTracker.put(episodeId, failed);
            if (trackingId != null) statusTracker.put(trackingId, failed);
            return failed;
        }
    }

    public EpisodePipelineStatusResponse resumeEpisode(UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        log.info("Reanudando episodio '{}' ({}) desde el estado actual: {}",
                episode.getTitle(), episodeId, episode.getStatus());

        UUID trackingId = registerPipeline(episodeId, episode.getTitle());
        return executePipeline(trackingId, episodeId, true);
    }

    public Shot regenerateShot(UUID shotId, String stage) {
        Shot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + shotId));

        String safeStage = stage != null ? stage.toUpperCase().trim() : "KEYFRAME";
        log.info("Regenerando plano {} en etapa {}", shotId, safeStage);

        return switch (safeStage) {
            case "NARRATION" -> narrationStageService.regenerateShotNarration(shotId);
            case "ANIMATION" -> animationStageService.regenerateShotAnimation(shotId);
            case "KEYFRAME", "KEYFRAMES" -> keyframeStageService.regenerateShotKeyframe(shotId);
            default -> keyframeStageService.regenerateShotKeyframe(shotId);
        };
    }

    private void updateStage(UUID trackingId, UUID episodeId, String title, String stage, int percent,
                             String message, String finalVideoPath, String error, OffsetDateTime startedAt, OffsetDateTime finishedAt) {
        EpisodePipelineStatusResponse status = new EpisodePipelineStatusResponse(
                episodeId,
                trackingId,
                title,
                stage,
                stage,
                percent,
                message,
                finalVideoPath,
                error,
                startedAt,
                finishedAt
        );

        if (episodeId != null) statusTracker.put(episodeId, status);
        if (trackingId != null) statusTracker.put(trackingId, status);
    }

    private int calculateProgressPercent(EpisodeStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case DRAFT -> 0;
            case PLANNING -> 5;
            case SAFETY_REVIEW -> 10;
            case AWAITING_SCRIPT_APPROVAL -> 15;
            case NARRATION -> 25;
            case KEYFRAMES -> 45;
            case ANIMATION -> 70;
            case SUBTITLES_OVERLAYS -> 80;
            case RENDERING -> 90;
            case VIDEO_QA -> 95;
            case EXPORTING -> 98;
            case COMPLETED -> 100;
            case FAILED -> 0;
        };
    }
}
