package com.storyvideo.api.video.timeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.audio.catalog.AudioCatalogService;
import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryScene;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.video.timeline.model.VideoTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {

    @Mock
    private AudioCatalogService audioCatalogService;

    @Mock
    private AssetStorageService assetStorageService;

    private ObjectMapper objectMapper;
    private TimelineService timelineService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        timelineService = new TimelineService(audioCatalogService, assetStorageService, objectMapper, 0.12);
    }

    @Test
    @DisplayName("Construye el timeline correctamente calculando duraciones, offsets y ducking")
    void shouldBuildTimelineCorrectly() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia Misteriosa", StoryGenre.MYSTERY, "Tema misterioso");
        story.setId(storyId);

        StoryScene scene1 = new StoryScene(1, "En un callejón oscuro...");
        scene1.setId(UUID.randomUUID());
        scene1.setImagePath("stories/" + storyId + "/images/scene_01.png");
        scene1.setNarrationAudioPath("stories/" + storyId + "/audio/narration/scene_01.mp3");
        scene1.setNarrationDurationSeconds(BigDecimal.valueOf(4.0));
        scene1.setCameraMovement(CameraMovement.SLOW_ZOOM_IN);

        StoryScene scene2 = new StoryScene(2, "Una sombra se desliza veloz...");
        scene2.setId(UUID.randomUUID());
        scene2.setImagePath("stories/" + storyId + "/images/scene_02.png");
        scene2.setNarrationAudioPath("stories/" + storyId + "/audio/narration/scene_02.mp3");
        scene2.setNarrationDurationSeconds(BigDecimal.valueOf(5.0));
        scene2.setCameraMovement(CameraMovement.KEN_BURNS);

        story.addScene(scene1);
        story.addScene(scene2);

        when(assetStorageService.resolveAbsolutePath(anyString()))
                .thenAnswer(inv -> Paths.get("C:/storage/" + inv.getArgument(0)));
        when(audioCatalogService.resolveBgmPath(any(StoryGenre.class)))
                .thenReturn(Paths.get("C:/audio/bgm/mystery.mp3"));
        when(audioCatalogService.resolveSfxPath(anyString()))
                .thenReturn(Paths.get("C:/audio/sfx/whoosh.mp3"));

        VideoTimeline timeline = timelineService.buildTimeline(story);

        assertThat(timeline).isNotNull();
        assertThat(timeline.storyId()).isEqualTo(storyId);
        assertThat(timeline.entries()).hasSize(2);

        // Escena 1
        var entry1 = timeline.entries().get(0);
        assertThat(entry1.sequenceNumber()).isEqualTo(1);
        assertThat(entry1.startTime()).isEqualTo(0.0);
        assertThat(entry1.narrationDuration()).isEqualTo(4.0);
        assertThat(entry1.narrationStartOffset()).isEqualTo(0.35);
        assertThat(entry1.duration()).isGreaterThan(4.0);
        assertThat(entry1.cameraMovement()).isEqualTo(CameraMovement.SLOW_ZOOM_IN);
        assertThat(entry1.imagePath()).contains("scene_01.png");
        assertThat(entry1.narrationAudioPath()).contains("scene_01.mp3");

        // Escena 2
        var entry2 = timeline.entries().get(1);
        assertThat(entry2.sequenceNumber()).isEqualTo(2);
        assertThat(entry2.startTime()).isEqualTo(entry1.endTime());
        assertThat(entry2.narrationDuration()).isEqualTo(5.0);
        assertThat(entry2.cameraMovement()).isEqualTo(CameraMovement.KEN_BURNS);

        // Audio Ducking
        assertThat(timeline.audioMix()).isNotNull();
        assertThat(timeline.audioMix().duckingPoints()).hasSize(2);
        assertThat(timeline.audioMix().duckingPoints().get(0).startTime()).isEqualTo(0.35);
        assertThat(timeline.audioMix().duckingPoints().get(0).endTime()).isEqualTo(4.35);

        // Duración total
        assertThat(timeline.totalDurationSeconds()).isEqualTo(entry2.endTime());
    }

    @Test
    @DisplayName("Serializa y deserializa el VideoTimeline en JSON bidireccionalmente")
    void shouldSerializeAndDeserializeJson() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia Sci-Fi", StoryGenre.SCI_FI, "Cyberpunk");
        story.setId(storyId);

        StoryScene scene = new StoryScene(1, "En el año 2099...");
        scene.setNarrationDurationSeconds(BigDecimal.valueOf(3.5));
        story.addScene(scene);

        when(audioCatalogService.resolveBgmPath(any(StoryGenre.class)))
                .thenReturn(Paths.get("C:/audio/bgm/scifi.mp3"));

        VideoTimeline original = timelineService.buildTimeline(story);
        String json = timelineService.toJson(original);

        assertThat(json).isNotBlank().contains("totalDurationSeconds");

        VideoTimeline restored = timelineService.fromJson(json);
        assertThat(restored.storyId()).isEqualTo(original.storyId());
        assertThat(restored.totalDurationSeconds()).isEqualTo(original.totalDurationSeconds());
        assertThat(restored.entries()).hasSize(original.entries().size());
    }

    @Test
    @DisplayName("Lanza excepción si la historia no tiene escenas")
    void shouldThrowExceptionWhenNoScenes() {
        Story story = new Story("Sin Escenas", StoryGenre.HORROR, "Vacio");
        assertThatThrownBy(() -> timelineService.buildTimeline(story))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sin escenas");
    }
}
