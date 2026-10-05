package com.kidsanim.api.pipeline;

import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Scene;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.ScenePurpose;
import com.kidsanim.api.dto.EpisodePipelineStatusResponse;
import com.kidsanim.api.export.dto.ExportResult;
import com.kidsanim.api.export.service.ExportService;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.ShotRepository;
import com.kidsanim.api.script.EpisodeScriptService;
import com.kidsanim.api.service.AnimationStageService;
import com.kidsanim.api.service.KeyframeStageService;
import com.kidsanim.api.service.NarrationStageService;
import com.kidsanim.api.service.RenderStageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EpisodePipelineExecutorTest {

    private EpisodeRepository episodeRepository;
    private ShotRepository shotRepository;
    private EpisodeScriptService episodeScriptService;
    private NarrationStageService narrationStageService;
    private KeyframeStageService keyframeStageService;
    private AnimationStageService animationStageService;
    private RenderStageService renderStageService;
    private ExportService exportService;

    private EpisodePipelineExecutor executor;

    @BeforeEach
    void setUp() {
        episodeRepository = mock(EpisodeRepository.class);
        shotRepository = mock(ShotRepository.class);
        episodeScriptService = mock(EpisodeScriptService.class);
        narrationStageService = mock(NarrationStageService.class);
        keyframeStageService = mock(KeyframeStageService.class);
        animationStageService = mock(AnimationStageService.class);
        renderStageService = mock(RenderStageService.class);
        exportService = mock(ExportService.class);

        executor = new EpisodePipelineExecutor(
                episodeRepository,
                shotRepository,
                episodeScriptService,
                narrationStageService,
                keyframeStageService,
                animationStageService,
                renderStageService,
                exportService
        );
    }

    @Test
    void executePipelineFullSuccess() {
        UUID epId = UUID.randomUUID();
        Series series = new Series("Tito el zorrito", "Aventuras", null);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "1 al 5", "Aprender a contar", "Contando con Tito");
        episode.setId(epId);
        episode.setStatus(EpisodeStatus.DRAFT);

        when(episodeRepository.findById(epId)).thenReturn(Optional.of(episode));
        when(episodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(narrationStageService.processEpisodeNarration(epId)).thenReturn(episode);
        when(keyframeStageService.processEpisodeKeyframes(epId)).thenReturn(episode);
        when(animationStageService.processEpisodeAnimation(epId)).thenReturn(episode);

        RenderStageService.EpisodeRenderResult renderResult = new RenderStageService.EpisodeRenderResult(
                episode, "/export/video.mp4", "/export/sub.ass", null
        );
        when(renderStageService.renderEpisodeWithResult(epId)).thenReturn(renderResult);

        ExportResult exportResult = new ExportResult(
                epId, "/export/video.mp4", "/export/thumb.png", "/export/meta.json", true, "OK"
        );
        when(exportService.exportEpisode(epId)).thenReturn(exportResult);

        EpisodePipelineStatusResponse response = executor.startEpisodePipeline(epId, true);

        assertThat(response).isNotNull();
        assertThat(response.episodeId()).isEqualTo(epId);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.progressPercent()).isEqualTo(100);
        assertThat(response.finalVideoPath()).isEqualTo("/export/video.mp4");

        verify(narrationStageService).processEpisodeNarration(epId);
        verify(keyframeStageService).processEpisodeKeyframes(epId);
        verify(animationStageService).processEpisodeAnimation(epId);
        verify(renderStageService).renderEpisodeWithResult(epId);
        verify(exportService).exportEpisode(epId);
    }

    @Test
    void resumeEpisodeStartsFromFirstIncompleteStage() {
        UUID epId = UUID.randomUUID();
        Series series = new Series("Tito el zorrito", "Aventuras", null);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "1 al 5", "Aprender", "Contando con Tito");
        episode.setId(epId);
        episode.setStatus(EpisodeStatus.NARRATION);

        Scene scene = new Scene(episode, 1, null, ScenePurpose.INTRO, "playful");
        Shot shot = new Shot(scene, 1, "Hola amigos", "Tito saludando");
        scene.getShots().add(shot);
        episode.getScenes().add(scene);

        when(episodeRepository.findById(epId)).thenReturn(Optional.of(episode));
        when(episodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(narrationStageService.processEpisodeNarration(epId)).thenReturn(episode);
        when(keyframeStageService.processEpisodeKeyframes(epId)).thenReturn(episode);
        when(animationStageService.processEpisodeAnimation(epId)).thenReturn(episode);

        RenderStageService.EpisodeRenderResult renderResult = new RenderStageService.EpisodeRenderResult(
                episode, "/export/resumed.mp4", "/export/sub.ass", null
        );
        when(renderStageService.renderEpisodeWithResult(epId)).thenReturn(renderResult);

        ExportResult exportResult = new ExportResult(
                epId, "/export/resumed.mp4", "/export/thumb.png", "/export/meta.json", true, "OK"
        );
        when(exportService.exportEpisode(epId)).thenReturn(exportResult);

        EpisodePipelineStatusResponse response = executor.resumeEpisode(epId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("COMPLETED");

        verify(narrationStageService).processEpisodeNarration(epId);
    }

    @Test
    void regenerateShotDelegatesToSpecificService() {
        UUID shotId = UUID.randomUUID();
        Shot shot = new Shot();
        shot.setId(shotId);

        when(shotRepository.findById(shotId)).thenReturn(Optional.of(shot));
        when(keyframeStageService.regenerateShotKeyframe(shotId)).thenReturn(shot);
        when(animationStageService.regenerateShotAnimation(shotId)).thenReturn(shot);
        when(narrationStageService.regenerateShotNarration(shotId)).thenReturn(shot);

        Shot shotKf = executor.regenerateShot(shotId, "KEYFRAME");
        assertThat(shotKf).isEqualTo(shot);
        verify(keyframeStageService).regenerateShotKeyframe(shotId);

        Shot shotAnim = executor.regenerateShot(shotId, "ANIMATION");
        assertThat(shotAnim).isEqualTo(shot);
        verify(animationStageService).regenerateShotAnimation(shotId);

        Shot shotNarr = executor.regenerateShot(shotId, "NARRATION");
        assertThat(shotNarr).isEqualTo(shot);
        verify(narrationStageService).regenerateShotNarration(shotId);
    }
}
