package com.storyvideo.api.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.domain.*;
import com.storyvideo.api.export.dto.ExportResult;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.VideoProjectRepository;
import com.storyvideo.api.repository.VideoRenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private VideoProjectRepository videoProjectRepository;

    @Mock
    private VideoRenderRepository videoRenderRepository;

    @Mock
    private AssetStorageService assetStorageService;

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exportService = new ExportService(
                tempDir.toString(),
                storyRepository,
                videoProjectRepository,
                videoRenderRepository,
                assetStorageService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Exporta el video MP4 y genera metadata.json conforme a la especificación")
    void shouldExportVideoAndGenerateMetadataJson() throws IOException {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("El Misterio de la Mina", StoryGenre.MYSTERY, "Mina abandonada");
        story.setId(storyId);
        story.setSynopsis("Un grupo de exploradores desciende a una mina...");
        story.setLanguage("es-MX");

        StoryScene scene = new StoryScene(1, "Entrada a la mina");
        story.addScene(scene);

        VideoProject project = new VideoProject(story);
        project.setId(UUID.randomUUID());
        project.setTotalDurationSeconds(BigDecimal.valueOf(45.50));

        VideoRender render = new VideoRender(project, 1);
        render.setId(UUID.randomUUID());
        render.setStatus(VideoRenderStatus.COMPLETED);
        render.setVideoPath("stories/" + storyId + "/renders/render_v1.mp4");
        render.setResolution("1080x1920");
        render.setCodec("h264");

        // Crear archivo fuente físico simulado
        Path sourceFile = tempDir.resolve("simulated_source.mp4");
        Files.writeString(sourceFile, "dummy mp4 content");

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(videoProjectRepository.findByStoryId(storyId)).thenReturn(Optional.of(project));
        when(videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(project.getId())).thenReturn(List.of(render));
        when(assetStorageService.resolveAbsolutePath(render.getVideoPath())).thenReturn(sourceFile);

        ExportResult result = exportService.exportStoryVideo(storyId);

        assertThat(result).isNotNull();
        assertThat(Files.exists(result.videoFilePath())).isTrue();
        assertThat(Files.exists(result.metadataFilePath())).isTrue();
        assertThat(result.metadata().title()).isEqualTo("El Misterio de la Mina");
        assertThat(result.metadata().genre()).isEqualTo("MYSTERY");
        assertThat(result.metadata().hashtags()).contains("#misterio", "#storytime", "#fyp");
        assertThat(result.metadata().resolution()).isEqualTo("1080x1920");
        assertThat(result.metadata().duration_seconds()).isEqualTo(45.50);

        assertThat(story.getStatus()).isEqualTo(StoryStatus.EXPORTED);
        verify(storyRepository).save(story);
    }

    @Test
    @DisplayName("Lanza excepción si no hay renders completados")
    void shouldThrowExceptionWhenNoCompletedRenders() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia Incompleta", StoryGenre.HORROR, "Terror");
        story.setId(storyId);

        VideoProject project = new VideoProject(story);
        project.setId(UUID.randomUUID());

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(videoProjectRepository.findByStoryId(storyId)).thenReturn(Optional.of(project));
        when(videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(project.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> exportService.exportStoryVideo(storyId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe ningún render completado");
    }

    @Test
    @DisplayName("Genera hashtags acordes al género de la historia")
    void shouldGenerateCorrectHashtagsForGenres() {
        Story horrorStory = new Story("Terror", StoryGenre.HORROR, "Fantasmas");
        List<String> horrorTags = exportService.generateHashtags(horrorStory);
        assertThat(horrorTags).contains("#horror", "#terror", "#storytime", "#fyp");

        Story scifiStory = new Story("Sci-Fi", StoryGenre.SCI_FI, "Robots");
        List<String> scifiTags = exportService.generateHashtags(scifiStory);
        assertThat(scifiTags).contains("#scifi", "#cienciaficcion", "#viral");
    }
}
