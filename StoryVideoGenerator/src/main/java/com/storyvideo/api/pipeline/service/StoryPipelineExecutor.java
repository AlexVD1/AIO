package com.storyvideo.api.pipeline.service;

import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.dto.VideoRenderResponseDto;
import com.storyvideo.api.export.dto.ExportResult;
import com.storyvideo.api.export.service.ExportService;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.pipeline.dto.PipelineStage;
import com.storyvideo.api.pipeline.dto.PipelineStatusDto;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.service.StoryAudioService;
import com.storyvideo.api.service.StoryGenerationService;
import com.storyvideo.api.service.StoryVisualService;
import com.storyvideo.api.service.VideoRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoryPipelineExecutor {

    private static final Logger log = LoggerFactory.getLogger(StoryPipelineExecutor.class);

    private final StoryGenerationService storyGenerationService;
    private final StoryVisualService storyVisualService;
    private final StoryAudioService storyAudioService;
    private final VideoRenderService videoRenderService;
    private final ExportService exportService;
    private final StoryRepository storyRepository;
    private final AssetStorageService assetStorageService;

    private final Map<UUID, PipelineStatusDto> statusTracker = new ConcurrentHashMap<>();

    public StoryPipelineExecutor(
            StoryGenerationService storyGenerationService,
            StoryVisualService storyVisualService,
            StoryAudioService storyAudioService,
            VideoRenderService videoRenderService,
            ExportService exportService,
            StoryRepository storyRepository,
            AssetStorageService assetStorageService) {
        this.storyGenerationService = storyGenerationService;
        this.storyVisualService = storyVisualService;
        this.storyAudioService = storyAudioService;
        this.videoRenderService = videoRenderService;
        this.exportService = exportService;
        this.storyRepository = storyRepository;
        this.assetStorageService = assetStorageService;
    }

    public UUID registerPipeline() {
        UUID trackingId = UUID.randomUUID();
        PipelineStatusDto initial = PipelineStatusDto.initial(trackingId);
        statusTracker.put(trackingId, initial);
        return trackingId;
    }

    @Async("storyTaskExecutor")
    public CompletableFuture<PipelineStatusDto> executeAsync(UUID trackingId, StoryGenerationRequestDto request) {
        return CompletableFuture.completedFuture(execute(trackingId, request, null));
    }

    @Async("storyTaskExecutor")
    public CompletableFuture<PipelineStatusDto> executeAsync(UUID trackingId, StoryGenerationRequestDto request, com.storyvideo.api.domain.BatchJob batchJob) {
        return CompletableFuture.completedFuture(execute(trackingId, request, batchJob));
    }

    public PipelineStatusDto execute(UUID trackingId, StoryGenerationRequestDto request, com.storyvideo.api.domain.BatchJob batchJob) {
        LocalDateTime startedAt = LocalDateTime.now();
        UUID storyId = null;

        try {
            // 1. Generación de historia estructurada
            updateStage(trackingId, null, PipelineStage.GENERATING_STORY, 15, "Generando historia con IA...", startedAt);
            StoryResponseDto storyDto = (batchJob != null)
                    ? storyGenerationService.generateAndPersistStory(request, batchJob)
                    : storyGenerationService.generateAndPersistStory(request);
            storyId = storyDto.id();

            return executeRemainingPipeline(trackingId, storyId, storyDto.title(), request.ttsVoice(), startedAt);

        } catch (Exception e) {
            log.error("Fallo durante la ejecución del pipeline (Tracking ID: {}): {}", trackingId, e.getMessage(), e);

            if (storyId != null) {
                try {
                    Story s = storyRepository.findById(storyId).orElse(null);
                    if (s != null) {
                        s.setStatus(StoryStatus.FAILED);
                        storyRepository.save(s);
                    }
                } catch (Exception ignored) {}
            }

            LocalDateTime completedAt = LocalDateTime.now();
            PipelineStatusDto failedStatus = new PipelineStatusDto(
                    storyId != null ? storyId : trackingId,
                    "FAILED",
                    PipelineStage.FAILED,
                    statusTracker.getOrDefault(trackingId, PipelineStatusDto.initial(trackingId)).progressPercent(),
                    "Error durante el pipeline: " + e.getMessage(),
                    null,
                    null,
                    List.of(e.getMessage() != null ? e.getMessage() : "Error desconocido"),
                    startedAt,
                    completedAt
            );

            statusTracker.put(trackingId, failedStatus);
            if (storyId != null) {
                statusTracker.put(storyId, failedStatus);
            }

            return failedStatus;
        }
    }

    public PipelineStatusDto executeExistingStory(UUID trackingId, UUID storyId) {
        LocalDateTime startedAt = LocalDateTime.now();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("Historia no encontrada: " + storyId));

        try {
            return executeRemainingPipeline(trackingId, storyId, story.getTitle(), null, startedAt);
        } catch (Exception e) {
            log.error("Fallo al reejecutar pipeline para historia existente (Story ID: {}): {}", storyId, e.getMessage(), e);
            story.setStatus(StoryStatus.FAILED);
            storyRepository.save(story);

            LocalDateTime completedAt = LocalDateTime.now();
            PipelineStatusDto failedStatus = new PipelineStatusDto(
                    storyId,
                    "FAILED",
                    PipelineStage.FAILED,
                    statusTracker.getOrDefault(trackingId, PipelineStatusDto.initial(trackingId)).progressPercent(),
                    "Error durante el pipeline: " + e.getMessage(),
                    null,
                    null,
                    List.of(e.getMessage() != null ? e.getMessage() : "Error desconocido"),
                    startedAt,
                    completedAt
            );
            statusTracker.put(trackingId, failedStatus);
            statusTracker.put(storyId, failedStatus);
            return failedStatus;
        }
    }

    private PipelineStatusDto executeRemainingPipeline(UUID trackingId, UUID storyId, String storyTitle, String ttsVoice, LocalDateTime startedAt) {
        // 2. Generación de imágenes (SD Forge local o fallback)
        updateStage(trackingId, storyId, PipelineStage.GENERATING_IMAGES, 35, "Generando imágenes para cada escena...", startedAt);
        storyVisualService.generateImagesForStory(storyId);

        // 3. Generación de narración y audio
        updateStage(trackingId, storyId, PipelineStage.GENERATING_AUDIO, 60, "Sintetizando locución con Edge-TTS...", startedAt);
        storyAudioService.generateAudioForStory(storyId, ttsVoice);

        // 4. Renderizado con FFmpeg
        updateStage(trackingId, storyId, PipelineStage.RENDERING_VIDEO, 80, "Componiendo y renderizando video en 1080x1920 con FFmpeg...", startedAt);
        VideoRenderResponseDto renderDto = videoRenderService.renderVideo(storyId);

        if (!renderDto.valid()) {
            throw new IllegalStateException("El video renderizado no superó la validación: " + String.join(", ", renderDto.validationIssues()));
        }

        // 5. Exportación y metadata para Make.com
        updateStage(trackingId, storyId, PipelineStage.EXPORTING, 95, "Exportando MP4 y generando metadata.json...", startedAt);
        ExportResult exportResult = exportService.exportStoryVideo(storyId);

        // 6. Completado
        LocalDateTime completedAt = LocalDateTime.now();
        PipelineStatusDto completedStatus = new PipelineStatusDto(
                storyId,
                "COMPLETED",
                PipelineStage.COMPLETED,
                100,
                "Video generado y exportado exitosamente",
                renderDto.videoUrl(),
                exportResult.videoFilePath().toString(),
                List.of(),
                startedAt,
                completedAt
        );

        statusTracker.put(trackingId, completedStatus);
        statusTracker.put(storyId, completedStatus);

        log.info("Pipeline completado exitosamente para historia '{}' (ID: {}) en {}s",
                storyTitle, storyId, java.time.Duration.between(startedAt, completedAt).toSeconds());

        return completedStatus;
    }

    public Optional<PipelineStatusDto> getStatus(UUID id) {
        if (statusTracker.containsKey(id)) {
            return Optional.of(statusTracker.get(id));
        }

        // Si no está en memoria, consultar estado persistido en base de datos
        return storyRepository.findById(id).map(story -> {
            int progress = switch (story.getStatus()) {
                case CREATED, PLANNING_SCENES -> 20;
                case GENERATING_ASSETS -> 50;
                case BUILDING_TIMELINE, RENDERING -> 80;
                case READY -> 90;
                case EXPORTED -> 100;
                case FAILED -> 0;
                default -> 10;
            };

            PipelineStage stage = switch (story.getStatus()) {
                case CREATED, PLANNING_SCENES -> PipelineStage.GENERATING_STORY;
                case GENERATING_ASSETS -> PipelineStage.GENERATING_IMAGES;
                case RENDERING -> PipelineStage.RENDERING_VIDEO;
                case READY, EXPORTED -> PipelineStage.COMPLETED;
                case FAILED -> PipelineStage.FAILED;
                default -> PipelineStage.NOT_STARTED;
            };

            return new PipelineStatusDto(
                    story.getId(),
                    story.getStatus().name(),
                    stage,
                    progress,
                    "Estado recuperado de la base de datos",
                    null,
                    null,
                    List.of(),
                    story.getCreatedAt(),
                    story.getUpdatedAt()
            );
        });
    }

    private void updateStage(UUID trackingId, UUID storyId, PipelineStage stage, int progress, String message, LocalDateTime startedAt) {
        PipelineStatusDto dto = new PipelineStatusDto(
                storyId != null ? storyId : trackingId,
                "PROCESSING",
                stage,
                progress,
                message,
                null,
                null,
                List.of(),
                startedAt,
                null
        );
        statusTracker.put(trackingId, dto);
        if (storyId != null) {
            statusTracker.put(storyId, dto);
        }
    }
}
