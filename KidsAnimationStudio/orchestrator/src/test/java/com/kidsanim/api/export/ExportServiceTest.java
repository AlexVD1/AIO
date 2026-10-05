package com.kidsanim.api.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.*;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.export.dto.ExportResult;
import com.kidsanim.api.export.service.ExportService;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExportServiceTest {

    @TempDir
    Path tempDir;

    private EpisodeRepository episodeRepository;
    private PipelineJobRepository pipelineJobRepository;
    private AssetRepository assetRepository;
    private KidsVideoProperties videoProperties;
    private ObjectMapper objectMapper;
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        episodeRepository = mock(EpisodeRepository.class);
        pipelineJobRepository = mock(PipelineJobRepository.class);
        assetRepository = mock(AssetRepository.class);
        videoProperties = mock(KidsVideoProperties.class);
        objectMapper = new ObjectMapper();

        when(videoProperties.exportPath()).thenReturn(tempDir.resolve("export_videos").toString());
        when(videoProperties.width()).thenReturn(1920);
        when(videoProperties.height()).thenReturn(1080);
        when(pipelineJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        exportService = new ExportService(
                episodeRepository,
                assetRepository,
                pipelineJobRepository,
                videoProperties,
                objectMapper
        );
    }

    @Test
    void exportEpisodeSuccess() throws Exception {
        UUID epId = UUID.randomUUID();
        Series series = new Series("Tito el Zorrito", "Aventuras", null);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Aprender los numeros", "Contar del 1 al 5", "Episodio 1: Contando manzanas");
        episode.setId(epId);
        episode.setDurationSeconds(new BigDecimal("30.0"));

        // Crear video origen simulado
        Path fakeVideo = tempDir.resolve("rendered_ep.mp4");
        Files.writeString(fakeVideo, "fake mp4 content");
        episode.setFinalVideoPath(fakeVideo.toString());

        // Crear keyframe simulado
        Path fakeKeyframe = tempDir.resolve("keyframe.png");
        Files.writeString(fakeKeyframe, "fake png content");

        Asset keyframeAsset = new Asset(episode, null, AssetType.KEYFRAME, fakeKeyframe.toString());

        when(episodeRepository.findById(epId)).thenReturn(Optional.of(episode));
        when(assetRepository.findByEpisodeId(epId)).thenReturn(List.of(keyframeAsset));
        when(assetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(episodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExportResult result = exportService.exportEpisode(epId);

        assertThat(result).isNotNull();
        assertThat(result.episodeId()).isEqualTo(epId);
        assertThat(result.exportedVideoPath()).isNotEmpty();
        assertThat(new File(result.exportedVideoPath())).exists();
        assertThat(result.thumbnailPath()).isNotEmpty();
        assertThat(new File(result.thumbnailPath())).exists();
        assertThat(result.metadataPath()).isNotEmpty();
        assertThat(new File(result.metadataPath())).exists();
        assertThat(result.success()).isTrue();

        assertThat(episode.getStatus()).isEqualTo(EpisodeStatus.COMPLETED);
    }

    @Test
    void exportEpisodeThrowsWhenVideoNotFound() {
        UUID epId = UUID.randomUUID();
        Episode episode = new Episode(null, EducationalTopicType.COUNTING, "Topic", "Objective", "Episodio sin video");
        episode.setId(epId);
        episode.setFinalVideoPath(null);

        when(episodeRepository.findById(epId)).thenReturn(Optional.of(episode));

        assertThatThrownBy(() -> exportService.exportEpisode(epId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El archivo de video final no existe en el disco");

        assertThat(episode.getStatus()).isEqualTo(EpisodeStatus.FAILED);
    }
}
