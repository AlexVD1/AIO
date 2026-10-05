package com.kidsanim.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.PipelineJob;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.PipelineJobStatus;
import com.kidsanim.api.domain.enums.PipelineStage;
import com.kidsanim.api.infrastructure.config.KidsPipelineProperties;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import com.kidsanim.api.video.ffmpeg.FFmpegCommandBuilder;
import com.kidsanim.api.video.ffmpeg.FFmpegExecutionException;
import com.kidsanim.api.video.ffmpeg.FFmpegProcessExecutor;
import com.kidsanim.api.video.subtitles.KaraokeSubtitleService;
import com.kidsanim.api.video.timeline.TimelineService;
import com.kidsanim.api.video.timeline.model.VideoTimeline;
import com.kidsanim.api.video.validation.QualityValidationService;
import com.kidsanim.api.video.validation.dto.VideoValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RenderStageService {

    private static final Logger log = LoggerFactory.getLogger(RenderStageService.class);

    private final EpisodeRepository episodeRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final AssetRepository assetRepository;
    private final TimelineService timelineService;
    private final KaraokeSubtitleService karaokeSubtitleService;
    private final FFmpegCommandBuilder ffmpegCommandBuilder;
    private final FFmpegProcessExecutor ffmpegProcessExecutor;
    private final QualityValidationService qualityValidationService;
    private final KidsPipelineProperties pipelineProperties;
    private final KidsVideoProperties videoProperties;
    private final ObjectMapper objectMapper;

    public record EpisodeRenderResult(
            Episode episode,
            String finalVideoPath,
            String subtitleAssPath,
            VideoValidationResult validationResult
    ) {}

    public RenderStageService(
            EpisodeRepository episodeRepository,
            PipelineJobRepository pipelineJobRepository,
            AssetRepository assetRepository,
            TimelineService timelineService,
            KaraokeSubtitleService karaokeSubtitleService,
            FFmpegCommandBuilder ffmpegCommandBuilder,
            FFmpegProcessExecutor ffmpegProcessExecutor,
            QualityValidationService qualityValidationService,
            KidsPipelineProperties pipelineProperties,
            KidsVideoProperties videoProperties,
            ObjectMapper objectMapper) {
        this.episodeRepository = episodeRepository;
        this.pipelineJobRepository = pipelineJobRepository;
        this.assetRepository = assetRepository;
        this.timelineService = timelineService;
        this.karaokeSubtitleService = karaokeSubtitleService;
        this.ffmpegCommandBuilder = ffmpegCommandBuilder;
        this.ffmpegProcessExecutor = ffmpegProcessExecutor;
        this.qualityValidationService = qualityValidationService;
        this.pipelineProperties = pipelineProperties;
        this.videoProperties = videoProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Episode renderEpisode(UUID episodeId) {
        return renderEpisodeWithResult(episodeId).episode();
    }

    @Transactional
    public EpisodeRenderResult renderEpisodeWithResult(UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        log.info("Iniciando etapas SUBTITLES_OVERLAYS y RENDERING para episodio '{}' ({})",
                episode.getTitle(), episodeId);

        // 1. Construir timeline maestro
        VideoTimeline timeline = timelineService.buildTimeline(episode);

        // 2. Etapa SUBTITLES_OVERLAYS
        Path subtitleAssPath = executeSubtitlesStage(episode, timeline);

        // 3. Etapa RENDERING
        EpisodeRenderResult renderResult = executeRenderingStage(episode, timeline, subtitleAssPath);

        return renderResult;
    }

    private Path executeSubtitlesStage(Episode episode, VideoTimeline timeline) {
        PipelineJob subJob = createOrGetJob(episode, PipelineStage.SUBTITLES_OVERLAYS);
        subJob.setStatus(PipelineJobStatus.RUNNING);
        subJob.setStartedAt(OffsetDateTime.now());
        subJob.setProgressPercent(20);
        pipelineJobRepository.save(subJob);

        episode.setStatus(EpisodeStatus.SUBTITLES_OVERLAYS);
        episodeRepository.save(episode);

        Path episodeDir = getEpisodeStoragePath(episode.getId());
        Path assPath = episodeDir.resolve("subtitles").resolve("karaoke.ass");

        try {
            karaokeSubtitleService.writeKaraokeAssFile(timeline, assPath);

            saveOrUpdateAsset(episode, AssetType.SUBTITLES, assPath.toAbsolutePath().toString(), Map.of(
                    "format", "ass",
                    "playResX", timeline.width(),
                    "playResY", timeline.height()
            ));

            subJob.setStatus(PipelineJobStatus.COMPLETED);
            subJob.setProgressPercent(100);
            subJob.setFinishedAt(OffsetDateTime.now());
            pipelineJobRepository.save(subJob);

            log.info("Etapa SUBTITLES_OVERLAYS completada exitosamente para episodio {}", episode.getId());
            return assPath;
        } catch (Exception e) {
            log.error("Fallo en etapa SUBTITLES_OVERLAYS para episodio {}: {}", episode.getId(), e.getMessage(), e);
            subJob.setStatus(PipelineJobStatus.FAILED);
            subJob.setErrorMessage(e.getMessage());
            subJob.setFinishedAt(OffsetDateTime.now());
            pipelineJobRepository.save(subJob);
            episode.setStatus(EpisodeStatus.FAILED);
            episodeRepository.save(episode);
            throw new RuntimeException("Error en etapa de subtítulos: " + e.getMessage(), e);
        }
    }

    private EpisodeRenderResult executeRenderingStage(Episode episode, VideoTimeline timeline, Path subtitleAssPath) {
        PipelineJob renderJob = createOrGetJob(episode, PipelineStage.RENDERING);
        renderJob.setStatus(PipelineJobStatus.RUNNING);
        renderJob.setStartedAt(OffsetDateTime.now());
        renderJob.setProgressPercent(40);
        pipelineJobRepository.save(renderJob);

        episode.setStatus(EpisodeStatus.RENDERING);
        episodeRepository.save(episode);

        Path episodeDir = getEpisodeStoragePath(episode.getId());
        Path videoDir = episodeDir.resolve("video");
        try {
            Files.createDirectories(videoDir);
        } catch (Exception ignored) {}

        Path finalVideoPath = videoDir.resolve(episode.getId() + "_final.mp4");

        // Intentar primero con el encoder preferido (ej. h264_nvenc)
        String preferredEncoder = videoProperties != null && videoProperties.encoder() != null
                ? videoProperties.encoder()
                : "h264_nvenc";
        String fallbackEncoder = videoProperties != null && videoProperties.fallbackEncoder() != null
                ? videoProperties.fallbackEncoder()
                : "libx264";

        try {
            List<String> command = ffmpegCommandBuilder.buildRenderCommandWithEncoder(timeline, finalVideoPath, subtitleAssPath, preferredEncoder);
            try {
                ffmpegProcessExecutor.execute(command);
            } catch (FFmpegExecutionException ex) {
                if (!fallbackEncoder.equalsIgnoreCase(preferredEncoder)) {
                    log.warn("Encoder preferido '{}' falló ({}), reintentando con fallback '{}'...",
                            preferredEncoder, ex.getMessage(), fallbackEncoder);
                    List<String> fallbackCmd = ffmpegCommandBuilder.buildRenderCommandWithEncoder(timeline, finalVideoPath, subtitleAssPath, fallbackEncoder);
                    ffmpegProcessExecutor.execute(fallbackCmd);
                } else {
                    throw ex;
                }
            }

            renderJob.setProgressPercent(80);
            pipelineJobRepository.save(renderJob);

            // Validar calidad con ffprobe
            VideoValidationResult validation = qualityValidationService.validate(
                    finalVideoPath,
                    timeline.totalDurationSeconds(),
                    timeline.width(),
                    timeline.height()
            );

            if (!validation.valid()) {
                log.warn("El video renderizado presenta advertencias de validación: {}", validation.issues());
            }

            // Registrar asset de FINAL_VIDEO
            saveOrUpdateAsset(episode, AssetType.FINAL_VIDEO, finalVideoPath.toAbsolutePath().toString(), Map.of(
                    "width", validation.width(),
                    "height", validation.height(),
                    "durationSeconds", validation.durationSeconds(),
                    "videoCodec", validation.videoCodec() != null ? validation.videoCodec() : "h264",
                    "audioCodec", validation.audioCodec() != null ? validation.audioCodec() : "aac",
                    "fileSizeBytes", validation.fileSizeBytes(),
                    "valid", validation.valid()
            ));

            episode.setFinalVideoPath(finalVideoPath.toAbsolutePath().toString());
            episode.setDurationSeconds(BigDecimal.valueOf(validation.durationSeconds()));
            episode.setStatus(validation.valid() ? EpisodeStatus.VIDEO_QA : EpisodeStatus.VIDEO_QA);
            Episode savedEpisode = episodeRepository.save(episode);

            renderJob.setStatus(PipelineJobStatus.COMPLETED);
            renderJob.setProgressPercent(100);
            renderJob.setFinishedAt(OffsetDateTime.now());
            pipelineJobRepository.save(renderJob);

            log.info("Etapa RENDERING completada para episodio '{}'. Video: {} ({}s). Estado: {}",
                    savedEpisode.getTitle(), finalVideoPath.getFileName(), validation.durationSeconds(), savedEpisode.getStatus());

            return new EpisodeRenderResult(savedEpisode, finalVideoPath.toAbsolutePath().toString(), subtitleAssPath.toAbsolutePath().toString(), validation);
        } catch (Exception e) {
            log.error("Fallo en etapa RENDERING para episodio {}: {}", episode.getId(), e.getMessage(), e);
            renderJob.setStatus(PipelineJobStatus.FAILED);
            renderJob.setErrorMessage(e.getMessage());
            renderJob.setFinishedAt(OffsetDateTime.now());
            pipelineJobRepository.save(renderJob);
            episode.setStatus(EpisodeStatus.FAILED);
            episodeRepository.save(episode);
            throw new RuntimeException("Error en etapa de renderizado: " + e.getMessage(), e);
        }
    }

    private PipelineJob createOrGetJob(Episode episode, PipelineStage stage) {
        return pipelineJobRepository.findByEpisodeIdAndStage(episode.getId(), stage)
                .orElseGet(() -> {
                    PipelineJob newJob = new PipelineJob(episode, stage);
                    return pipelineJobRepository.save(newJob);
                });
    }

    private void saveOrUpdateAsset(Episode episode, AssetType type, String path, Map<String, Object> metadata) {
        String metaJson = null;
        try {
            if (metadata != null) {
                metaJson = objectMapper.writeValueAsString(metadata);
            }
        } catch (Exception ignored) {}

        List<Asset> existingAssets = assetRepository.findByEpisodeIdAndType(episode.getId(), type);
        Asset asset;
        if (existingAssets != null && !existingAssets.isEmpty()) {
            asset = existingAssets.get(0);
            asset.setPath(path);
            asset.setMetaJson(metaJson);
        } else {
            asset = new Asset(episode, type, path);
            asset.setMetaJson(metaJson);
        }
        assetRepository.save(asset);
    }

    private Path getEpisodeStoragePath(UUID episodeId) {
        String base = pipelineProperties != null && pipelineProperties.storagePath() != null
                ? pipelineProperties.storagePath()
                : "./storage";
        return Paths.get(base).resolve("episodes").resolve(episodeId.toString()).toAbsolutePath().normalize();
    }
}
