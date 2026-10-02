package com.storyvideo.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.domain.*;
import com.storyvideo.api.dto.VideoRenderResponseDto;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.VideoProjectRepository;
import com.storyvideo.api.repository.VideoRenderRepository;
import com.storyvideo.api.video.ffmpeg.FFmpegCommandBuilder;
import com.storyvideo.api.video.ffmpeg.FFmpegProcessExecutor;
import com.storyvideo.api.video.timeline.model.VideoTimeline;
import com.storyvideo.api.video.timeline.service.TimelineService;
import com.storyvideo.api.video.validation.QualityValidationService;
import com.storyvideo.api.video.validation.dto.VideoValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VideoRenderService {

    private static final Logger log = LoggerFactory.getLogger(VideoRenderService.class);

    private final StoryRepository storyRepository;
    private final VideoProjectRepository videoProjectRepository;
    private final VideoRenderRepository videoRenderRepository;
    private final TimelineService timelineService;
    private final FFmpegCommandBuilder ffmpegCommandBuilder;
    private final FFmpegProcessExecutor ffmpegProcessExecutor;
    private final QualityValidationService qualityValidationService;
    private final AssetStorageService assetStorageService;
    private final com.storyvideo.api.video.subtitles.service.SubtitleService subtitleService;
    private final ObjectMapper objectMapper;

    public VideoRenderService(
            StoryRepository storyRepository,
            VideoProjectRepository videoProjectRepository,
            VideoRenderRepository videoRenderRepository,
            TimelineService timelineService,
            FFmpegCommandBuilder ffmpegCommandBuilder,
            FFmpegProcessExecutor ffmpegProcessExecutor,
            QualityValidationService qualityValidationService,
            AssetStorageService assetStorageService,
            com.storyvideo.api.video.subtitles.service.SubtitleService subtitleService,
            ObjectMapper objectMapper) {
        this.storyRepository = storyRepository;
        this.videoProjectRepository = videoProjectRepository;
        this.videoRenderRepository = videoRenderRepository;
        this.timelineService = timelineService;
        this.ffmpegCommandBuilder = ffmpegCommandBuilder;
        this.ffmpegProcessExecutor = ffmpegProcessExecutor;
        this.qualityValidationService = qualityValidationService;
        this.assetStorageService = assetStorageService;
        this.subtitleService = subtitleService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VideoRenderResponseDto renderVideo(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("Historia no encontrada con ID: " + storyId));

        if (story.getScenes() == null || story.getScenes().isEmpty()) {
            throw new IllegalStateException("La historia no tiene escenas configuradas para renderizar");
        }

        // 1. Obtener o crear proyecto de video
        VideoProject project = videoProjectRepository.findByStoryId(storyId)
                .orElseGet(() -> {
                    VideoProject newProj = new VideoProject(story);
                    return videoProjectRepository.save(newProj);
                });

        // 2. Construir Timeline y persistir en el proyecto
        VideoTimeline timeline = timelineService.buildTimeline(story);
        String timelineJson = timelineService.toJson(timeline);

        project.setTimelineJson(timelineJson);
        project.setTotalDurationSeconds(BigDecimal.valueOf(timeline.totalDurationSeconds()));
        project.setStatus("RENDERING");
        project = videoProjectRepository.save(project);

        // 3. Crear registro de render
        List<VideoRender> previousRenders = videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(project.getId());
        int nextAttempt = previousRenders.size() + 1;

        VideoRender render = new VideoRender(project, nextAttempt);
        render.setStartedAt(LocalDateTime.now());
        render.setStatus(VideoRenderStatus.RENDERING);
        render = videoRenderRepository.save(render);

        // 4. Preparar ruta de salida física
        String relativeVideoPath = String.format("stories/%s/renders/render_v%d.mp4", storyId, nextAttempt);
        Path outputAbsPath = assetStorageService.resolveAbsolutePath(relativeVideoPath);
        try {
            Files.createDirectories(outputAbsPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio para almacenar el render", e);
        }

        // 5. Generar archivo ASS de subtítulos sincronizados (y SRT de respaldo)
        Path subAbsPath = null;
        try {
            String relativeAssPath = String.format("stories/%s/subtitles/story.ass", storyId);
            Path targetAss = assetStorageService.resolveAbsolutePath(relativeAssPath);
            subAbsPath = subtitleService.writeAssFile(story, timeline, targetAss);

            String relativeSrtPath = String.format("stories/%s/subtitles/story.srt", storyId);
            Path targetSrt = assetStorageService.resolveAbsolutePath(relativeSrtPath);
            subtitleService.writeSrtFile(story, timeline, targetSrt);
        } catch (Exception e) {
            log.warn("No se pudieron generar subtítulos para la historia {}: {}", storyId, e.getMessage());
        }

        // 6. Construir y ejecutar comando FFmpeg
        List<String> command = ffmpegCommandBuilder.buildRenderCommand(timeline, outputAbsPath, subAbsPath, story.getGenre());

        VideoValidationResult validation = null;
        try {
            ffmpegProcessExecutor.execute(command);

            // 6. Validar calidad del video renderizado
            validation = qualityValidationService.validate(outputAbsPath, timeline.totalDurationSeconds());
            String validationJson = objectMapper.writeValueAsString(validation);

            render.setValidationResultJson(validationJson);
            render.setVideoPath(relativeVideoPath);
            render.setResolution(validation.width() + "x" + validation.height());
            render.setCodec(validation.videoCodec());
            render.setCompletedAt(LocalDateTime.now());

            if (validation.valid()) {
                render.setStatus(VideoRenderStatus.COMPLETED);
                project.setStatus("COMPLETED");
                story.setStatus(StoryStatus.READY);
                storyRepository.save(story);
                log.info("Video renderizado y validado con éxito para historia '{}' (Intento #{})", story.getTitle(), nextAttempt);
            } else {
                render.setStatus(VideoRenderStatus.FAILED);
                render.setErrorMessage("Incidencias de calidad detectadas: " + String.join("; ", validation.issues()));
                project.setStatus("FAILED");
                log.warn("Video renderizado pero no superó la validación de calidad: {}", validation.issues());
            }

            videoRenderRepository.save(render);
            videoProjectRepository.save(project);

        } catch (Exception e) {
            render.setStatus(VideoRenderStatus.FAILED);
            render.setErrorMessage(e.getMessage());
            render.setCompletedAt(LocalDateTime.now());
            videoRenderRepository.save(render);

            project.setStatus("FAILED");
            videoProjectRepository.save(project);
            log.error("Error durante el renderizado del video para historia {}: {}", storyId, e.getMessage());
            throw new RuntimeException("Error al renderizar el video: " + e.getMessage(), e);
        }

        String publicUrl = render.getVideoPath() != null ? assetStorageService.buildPublicUrl(render.getVideoPath()) : null;

        return new VideoRenderResponseDto(
                render.getId(),
                project.getId(),
                story.getId(),
                render.getAttemptNumber(),
                render.getStatus().name(),
                publicUrl,
                validation != null ? validation.actualDuration() : timeline.totalDurationSeconds(),
                render.getResolution(),
                render.getCodec(),
                validation != null && validation.valid(),
                validation != null ? validation.issues() : List.of(),
                render.getErrorMessage(),
                render.getStartedAt(),
                render.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public VideoRenderResponseDto getLatestRender(UUID storyId) {
        VideoProject project = videoProjectRepository.findByStoryId(storyId)
                .orElseThrow(() -> new IllegalArgumentException("No existe proyecto de video para la historia: " + storyId));

        List<VideoRender> renders = videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(project.getId());
        if (renders.isEmpty()) {
            throw new IllegalStateException("El proyecto de video no cuenta con renders ejecutados");
        }

        VideoRender latest = renders.get(renders.size() - 1);
        String publicUrl = latest.getVideoPath() != null ? assetStorageService.buildPublicUrl(latest.getVideoPath()) : null;

        return new VideoRenderResponseDto(
                latest.getId(),
                project.getId(),
                storyId,
                latest.getAttemptNumber(),
                latest.getStatus().name(),
                publicUrl,
                project.getTotalDurationSeconds() != null ? project.getTotalDurationSeconds().doubleValue() : 0.0,
                latest.getResolution(),
                latest.getCodec(),
                latest.getStatus() == VideoRenderStatus.COMPLETED,
                List.of(),
                latest.getErrorMessage(),
                latest.getStartedAt(),
                latest.getCompletedAt()
        );
    }
}
