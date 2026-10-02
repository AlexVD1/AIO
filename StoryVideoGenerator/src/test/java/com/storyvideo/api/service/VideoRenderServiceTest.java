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
import com.storyvideo.api.video.ffmpeg.exception.FFmpegExecutionException;
import com.storyvideo.api.video.timeline.model.AudioMixPlan;
import com.storyvideo.api.video.timeline.model.TimelineEntry;
import com.storyvideo.api.video.timeline.model.VideoTimeline;
import com.storyvideo.api.video.timeline.service.TimelineService;
import com.storyvideo.api.video.validation.QualityValidationService;
import com.storyvideo.api.video.validation.dto.VideoValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoRenderServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private VideoProjectRepository videoProjectRepository;

    @Mock
    private VideoRenderRepository videoRenderRepository;

    @Mock
    private TimelineService timelineService;

    @Mock
    private FFmpegCommandBuilder ffmpegCommandBuilder;

    @Mock
    private FFmpegProcessExecutor ffmpegProcessExecutor;

    @Mock
    private QualityValidationService qualityValidationService;

    @Mock
    private AssetStorageService assetStorageService;

    @Mock
    private com.storyvideo.api.video.subtitles.service.SubtitleService subtitleService;

    private ObjectMapper objectMapper;
    private VideoRenderService videoRenderService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        videoRenderService = new VideoRenderService(
                storyRepository,
                videoProjectRepository,
                videoRenderRepository,
                timelineService,
                ffmpegCommandBuilder,
                ffmpegProcessExecutor,
                qualityValidationService,
                assetStorageService,
                subtitleService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Renderiza video exitosamente y actualiza estado a COMPLETED y READY")
    void shouldRenderVideoSuccessfully() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia Exitosa", StoryGenre.HORROR, "Terror");
        story.setId(storyId);
        StoryScene scene = new StoryScene(1, "Oscuridad absoluta");
        story.addScene(scene);

        VideoProject project = new VideoProject(story);
        project.setId(UUID.randomUUID());

        VideoTimeline timeline = new VideoTimeline(storyId, 5.0, List.of(
                new TimelineEntry(UUID.randomUUID(), 1, 0.0, 5.0, 5.0, null,
                        CameraMovement.SLOW_ZOOM_IN, TransitionType.CUT, TransitionType.CUT, 0.0,
                        List.of(), null, 0.0, 0.0, List.of(), List.of())
        ), new AudioMixPlan(null, 0.12, List.of(), List.of()));

        Path mockOutputPath = Paths.get("target/test-render.mp4");

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(videoProjectRepository.findByStoryId(storyId)).thenReturn(Optional.of(project));
        when(timelineService.buildTimeline(story)).thenReturn(timeline);
        when(timelineService.toJson(timeline)).thenReturn("{\"timeline\": true}");
        when(videoProjectRepository.save(any(VideoProject.class))).thenAnswer(inv -> inv.getArgument(0));
        when(videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(any())).thenReturn(List.of());
        when(videoRenderRepository.save(any(VideoRender.class))).thenAnswer(inv -> {
            VideoRender r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(assetStorageService.resolveAbsolutePath(anyString())).thenReturn(mockOutputPath);
        when(ffmpegCommandBuilder.buildRenderCommand(eq(timeline), any(Path.class), any(), any())).thenReturn(List.of("ffmpeg", "-version"));
        when(ffmpegProcessExecutor.execute(anyList())).thenReturn(new FFmpegProcessExecutor.ExecutionResult(0, Duration.ofSeconds(2), List.of()));
        when(qualityValidationService.validate(any(Path.class), eq(5.0))).thenReturn(
                new VideoValidationResult(true, List.of(), 5.0, "h264", "aac", 1080, 1920, 204800, "{}")
        );
        when(assetStorageService.buildPublicUrl(anyString())).thenReturn("http://localhost:8081/assets/renders/video.mp4");

        VideoRenderResponseDto result = videoRenderService.renderVideo(storyId);

        assertThat(result).isNotNull();
        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.videoUrl()).isEqualTo("http://localhost:8081/assets/renders/video.mp4");
        assertThat(story.getStatus()).isEqualTo(StoryStatus.READY);

        verify(storyRepository).save(story);
        verify(videoProjectRepository, atLeastOnce()).save(project);
        verify(videoRenderRepository, atLeastOnce()).save(any(VideoRender.class));
    }

    @Test
    @DisplayName("Maneja fallo de ejecución de FFmpeg actualizando el render a FAILED")
    void shouldHandleFFmpegExecutionFailure() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia Fallida", StoryGenre.HORROR, "Terror");
        story.setId(storyId);
        story.addScene(new StoryScene(1, "Texto"));

        VideoProject project = new VideoProject(story);
        project.setId(UUID.randomUUID());

        VideoTimeline timeline = new VideoTimeline(storyId, 5.0, List.of(), null);
        Path mockOutputPath = Paths.get("target/test-fail.mp4");

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(videoProjectRepository.findByStoryId(storyId)).thenReturn(Optional.of(project));
        when(timelineService.buildTimeline(story)).thenReturn(timeline);
        when(videoProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(any())).thenReturn(List.of());
        when(videoRenderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetStorageService.resolveAbsolutePath(anyString())).thenReturn(mockOutputPath);
        when(ffmpegCommandBuilder.buildRenderCommand(any(), any(), any(), any())).thenReturn(List.of("ffmpeg"));
        when(ffmpegProcessExecutor.execute(anyList()))
                .thenThrow(new FFmpegExecutionException("Error simulado de FFmpeg", 1, "Filter error"));

        assertThatThrownBy(() -> videoRenderService.renderVideo(storyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al renderizar el video");

        ArgumentCaptor<VideoRender> renderCaptor = ArgumentCaptor.forClass(VideoRender.class);
        verify(videoRenderRepository, atLeastOnce()).save(renderCaptor.capture());
        VideoRender lastSavedRender = renderCaptor.getValue();
        assertThat(lastSavedRender.getStatus()).isEqualTo(VideoRenderStatus.FAILED);
        assertThat(lastSavedRender.getErrorMessage()).contains("Error simulado de FFmpeg");
    }

    @Test
    @DisplayName("Obtiene el último render ejecutado para una historia")
    void shouldGetLatestRender() {
        UUID storyId = UUID.randomUUID();
        VideoProject project = new VideoProject(new Story("Historia", StoryGenre.HORROR, "Test"));
        project.setId(UUID.randomUUID());

        VideoRender r1 = new VideoRender(project, 1);
        r1.setId(UUID.randomUUID());
        r1.setStatus(VideoRenderStatus.FAILED);

        VideoRender r2 = new VideoRender(project, 2);
        r2.setId(UUID.randomUUID());
        r2.setStatus(VideoRenderStatus.COMPLETED);
        r2.setVideoPath("stories/test/render_v2.mp4");

        when(videoProjectRepository.findByStoryId(storyId)).thenReturn(Optional.of(project));
        when(videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(project.getId()))
                .thenReturn(List.of(r1, r2));
        when(assetStorageService.buildPublicUrl("stories/test/render_v2.mp4"))
                .thenReturn("http://localhost:8081/assets/stories/test/render_v2.mp4");

        VideoRenderResponseDto latest = videoRenderService.getLatestRender(storyId);

        assertThat(latest).isNotNull();
        assertThat(latest.attemptNumber()).isEqualTo(2);
        assertThat(latest.status()).isEqualTo("COMPLETED");
        assertThat(latest.videoUrl()).contains("render_v2.mp4");
    }
}
