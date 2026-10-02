package com.storyvideo.api.pipeline.service;

import com.storyvideo.api.domain.NarrativeArc;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.domain.StoryTone;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.dto.VideoRenderResponseDto;
import com.storyvideo.api.export.dto.ExportMetadata;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryPipelineExecutorTest {

    @Mock
    private StoryGenerationService storyGenerationService;

    @Mock
    private StoryVisualService storyVisualService;

    @Mock
    private StoryAudioService storyAudioService;

    @Mock
    private VideoRenderService videoRenderService;

    @Mock
    private ExportService exportService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private AssetStorageService assetStorageService;

    private StoryPipelineExecutor pipelineExecutor;

    @BeforeEach
    void setUp() {
        pipelineExecutor = new StoryPipelineExecutor(
                storyGenerationService,
                storyVisualService,
                storyAudioService,
                videoRenderService,
                exportService,
                storyRepository,
                assetStorageService
        );
    }

    @Test
    @DisplayName("Ejecuta el pipeline completo de punta a punta exitosamente")
    void shouldExecutePipelineEndToEndSuccessfully() throws Exception {
        UUID storyId = UUID.randomUUID();
        StoryGenerationRequestDto request = new StoryGenerationRequestDto(
                StoryGenre.HORROR, StoryTone.DARK, "Mansión", null, 60, "es-MX", 4, null, null
        );

        StoryResponseDto storyDto = new StoryResponseDto(
                storyId, "Historia", StoryGenre.HORROR, null, StoryTone.DARK, "Mansión",
                "Hook", "Premisa", "Sinopsis", NarrativeArc.LINEAR, null, "Fin",
                "es-MX", 60, StoryStatus.PLANNING_SCENES, 1, null, List.of(), List.of(), LocalDateTime.now()
        );

        VideoRenderResponseDto renderDto = new VideoRenderResponseDto(
                UUID.randomUUID(), UUID.randomUUID(), storyId, 1, "COMPLETED",
                "http://localhost:8081/assets/render.mp4", 60.0, "1080x1920", "h264",
                true, List.of(), null, LocalDateTime.now(), LocalDateTime.now()
        );

        ExportMetadata meta = new ExportMetadata(
                storyId, "Historia", "Desc", List.of("#horror"), "HORROR", "Entertainment",
                "es-MX", 60.0, "1080x1920", "mp4", "h264", "aac", 30,
                "2026-10-02T12:00:00", "Historia.mp4", 4, false, true, true, "es-MX-JorgeNeural"
        );
        ExportResult exportResult = new ExportResult(Paths.get("export/Historia.mp4"), Paths.get("export/Historia_metadata.json"), meta);

        when(storyGenerationService.generateAndPersistStory(request)).thenReturn(storyDto);
        when(storyVisualService.generateImagesForStory(storyId)).thenReturn(storyDto);
        when(storyAudioService.generateAudioForStory(storyId, null)).thenReturn(storyDto);
        when(videoRenderService.renderVideo(storyId)).thenReturn(renderDto);
        when(exportService.exportStoryVideo(storyId)).thenReturn(exportResult);

        UUID trackingId = pipelineExecutor.registerPipeline();
        CompletableFuture<PipelineStatusDto> future = pipelineExecutor.executeAsync(trackingId, request);
        PipelineStatusDto result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.currentStage()).isEqualTo(PipelineStage.COMPLETED);
        assertThat(result.progressPercent()).isEqualTo(100);
        assertThat(result.videoUrl()).isEqualTo("http://localhost:8081/assets/render.mp4");
        assertThat(result.exportPath()).contains("Historia.mp4");

        verify(storyGenerationService).generateAndPersistStory(request);
        verify(storyVisualService).generateImagesForStory(storyId);
        verify(storyAudioService).generateAudioForStory(storyId, null);
        verify(videoRenderService).renderVideo(storyId);
        verify(exportService).exportStoryVideo(storyId);
    }

    @Test
    @DisplayName("Maneja error en una etapa y marca el estado del pipeline como FAILED")
    void shouldHandlePipelineStageFailure() throws Exception {
        UUID storyId = UUID.randomUUID();
        StoryGenerationRequestDto request = new StoryGenerationRequestDto(
                StoryGenre.HORROR, StoryTone.DARK, "Mansión", null, 60, "es-MX", 4, null, null
        );

        StoryResponseDto storyDto = new StoryResponseDto(
                storyId, "Historia", StoryGenre.HORROR, null, StoryTone.DARK, "Mansión",
                "Hook", "Premisa", "Sinopsis", NarrativeArc.LINEAR, null, "Fin",
                "es-MX", 60, StoryStatus.PLANNING_SCENES, 1, null, List.of(), List.of(), LocalDateTime.now()
        );

        when(storyGenerationService.generateAndPersistStory(request)).thenReturn(storyDto);
        when(storyVisualService.generateImagesForStory(storyId))
                .thenThrow(new RuntimeException("Error simulado al generar imágenes"));

        UUID trackingId = pipelineExecutor.registerPipeline();
        CompletableFuture<PipelineStatusDto> future = pipelineExecutor.executeAsync(trackingId, request);
        PipelineStatusDto result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.currentStage()).isEqualTo(PipelineStage.FAILED);
        assertThat(result.errors()).anyMatch(err -> err.contains("Error simulado al generar imágenes"));
    }
}
