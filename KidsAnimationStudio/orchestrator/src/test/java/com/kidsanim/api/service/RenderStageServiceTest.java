package com.kidsanim.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.*;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.PipelineJobStatus;
import com.kidsanim.api.infrastructure.config.KidsPipelineProperties;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import com.kidsanim.api.video.ffmpeg.FFmpegCommandBuilder;
import com.kidsanim.api.video.ffmpeg.FFmpegExecutionException;
import com.kidsanim.api.video.ffmpeg.FFmpegProcessExecutor;
import com.kidsanim.api.video.subtitles.KaraokeSubtitleService;
import com.kidsanim.api.video.timeline.TimelineService;
import com.kidsanim.api.video.timeline.model.AudioMixPlan;
import com.kidsanim.api.video.timeline.model.VideoTimeline;
import com.kidsanim.api.video.validation.QualityValidationService;
import com.kidsanim.api.video.validation.dto.VideoValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RenderStageServiceTest {

    private EpisodeRepository episodeRepository;
    private PipelineJobRepository pipelineJobRepository;
    private AssetRepository assetRepository;
    private TimelineService timelineService;
    private KaraokeSubtitleService karaokeSubtitleService;
    private FFmpegCommandBuilder ffmpegCommandBuilder;
    private FFmpegProcessExecutor ffmpegProcessExecutor;
    private QualityValidationService qualityValidationService;
    private KidsPipelineProperties pipelineProperties;
    private KidsVideoProperties videoProperties;
    private ObjectMapper objectMapper;
    private RenderStageService renderStageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        episodeRepository = mock(EpisodeRepository.class);
        pipelineJobRepository = mock(PipelineJobRepository.class);
        assetRepository = mock(AssetRepository.class);
        timelineService = mock(TimelineService.class);
        karaokeSubtitleService = mock(KaraokeSubtitleService.class);
        ffmpegCommandBuilder = mock(FFmpegCommandBuilder.class);
        ffmpegProcessExecutor = mock(FFmpegProcessExecutor.class);
        qualityValidationService = mock(QualityValidationService.class);

        pipelineProperties = new KidsPipelineProperties(tempDir.toString(), "ef_dora", "-12%");
        videoProperties = new KidsVideoProperties(
                "ffmpeg", "ffprobe", 1920, 1080, 30,
                "h264_nvenc", "libx264", "medium", 22,
                900L, tempDir.resolve("audio").toString(), 0.12,
                tempDir.resolve("export").toString(), "Arial"
        );
        objectMapper = new ObjectMapper();

        renderStageService = new RenderStageService(
                episodeRepository, pipelineJobRepository, assetRepository,
                timelineService, karaokeSubtitleService, ffmpegCommandBuilder,
                ffmpegProcessExecutor, qualityValidationService,
                pipelineProperties, videoProperties, objectMapper
        );
    }

    @Test
    void renderEpisodeExecutesSubtitlesAndRenderingStagesSuccessfully() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "Aprender", "Episodio 1");
        episode.setId(episodeId);
        episode.setStatus(EpisodeStatus.SUBTITLES_OVERLAYS);

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(pipelineJobRepository.findByEpisodeIdAndStage(any(), any()))
                .thenReturn(Optional.empty());
        when(pipelineJobRepository.save(any(PipelineJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoTimeline timeline = new VideoTimeline(
                episodeId, 12.0, 1920, 1080, 30,
                List.of(), new AudioMixPlan(null, 0.12, List.of(), List.of())
        );
        when(timelineService.buildTimeline(episode)).thenReturn(timeline);

        Path mockAssPath = tempDir.resolve("subtitles.ass");
        Files.createFile(mockAssPath);
        when(karaokeSubtitleService.writeKaraokeAssFile(eq(timeline), any())).thenReturn(mockAssPath);

        when(ffmpegCommandBuilder.buildRenderCommandWithEncoder(eq(timeline), any(), any(), eq("h264_nvenc")))
                .thenReturn(List.of("ffmpeg", "-i", "input", "output.mp4"));

        when(ffmpegProcessExecutor.execute(any()))
                .thenReturn(new FFmpegProcessExecutor.ExecutionResult(0, Duration.ofSeconds(2), List.of()));

        VideoValidationResult mockValidation = new VideoValidationResult(
                true, List.of(), 12.0, "h264", "aac", 1920, 1080, 5000000L, "{}"
        );
        when(qualityValidationService.validate(any(), eq(12.0), eq(1920), eq(1080)))
                .thenReturn(mockValidation);

        Episode result = renderStageService.renderEpisode(episodeId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EpisodeStatus.VIDEO_QA);
        assertThat(result.getFinalVideoPath()).isNotNull();
        assertThat(result.getDurationSeconds()).isNotNull();

        verify(karaokeSubtitleService, times(1)).writeKaraokeAssFile(eq(timeline), any());
        verify(ffmpegProcessExecutor, times(1)).execute(any());
        verify(qualityValidationService, times(1)).validate(any(), eq(12.0), eq(1920), eq(1080));
        // Se guardan los assets de SUBTITLES y FINAL_VIDEO
        verify(assetRepository, times(2)).save(any(Asset.class));
    }

    @Test
    void renderEpisodeRetriesWithFallbackEncoderWhenNvencFails() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "Aprender", "Episodio 1");
        episode.setId(episodeId);

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pipelineJobRepository.findByEpisodeIdAndStage(any(), any())).thenReturn(Optional.empty());
        when(pipelineJobRepository.save(any(PipelineJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoTimeline timeline = new VideoTimeline(
                episodeId, 6.0, 1920, 1080, 30,
                List.of(), new AudioMixPlan(null, 0.12, List.of(), List.of())
        );
        when(timelineService.buildTimeline(episode)).thenReturn(timeline);

        Path mockAssPath = tempDir.resolve("subtitles2.ass");
        Files.createFile(mockAssPath);
        when(karaokeSubtitleService.writeKaraokeAssFile(eq(timeline), any())).thenReturn(mockAssPath);

        List<String> nvencCmd = List.of("ffmpeg", "-c:v", "h264_nvenc");
        List<String> libx264Cmd = List.of("ffmpeg", "-c:v", "libx264");

        when(ffmpegCommandBuilder.buildRenderCommandWithEncoder(eq(timeline), any(), any(), eq("h264_nvenc")))
                .thenReturn(nvencCmd);
        when(ffmpegCommandBuilder.buildRenderCommandWithEncoder(eq(timeline), any(), any(), eq("libx264")))
                .thenReturn(libx264Cmd);

        // Primer intento con h264_nvenc falla con FFmpegExecutionException
        when(ffmpegProcessExecutor.execute(nvencCmd))
                .thenThrow(new FFmpegExecutionException("NVENC out of memory or not supported", -1, "error"));
        // Segundo intento con libx264 triunfa
        when(ffmpegProcessExecutor.execute(libx264Cmd))
                .thenReturn(new FFmpegProcessExecutor.ExecutionResult(0, Duration.ofSeconds(3), List.of()));

        VideoValidationResult mockValidation = new VideoValidationResult(
                true, List.of(), 6.0, "h264", "aac", 1920, 1080, 2000000L, "{}"
        );
        when(qualityValidationService.validate(any(), eq(6.0), eq(1920), eq(1080)))
                .thenReturn(mockValidation);

        Episode result = renderStageService.renderEpisode(episodeId);

        assertThat(result.getStatus()).isEqualTo(EpisodeStatus.VIDEO_QA);
        verify(ffmpegProcessExecutor, times(1)).execute(nvencCmd);
        verify(ffmpegProcessExecutor, times(1)).execute(libx264Cmd);
    }
}
