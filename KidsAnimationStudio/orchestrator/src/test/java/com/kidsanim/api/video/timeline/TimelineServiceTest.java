package com.kidsanim.api.video.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.audio.AudioCatalogService;
import com.kidsanim.api.domain.*;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.SceneRepository;
import com.kidsanim.api.repository.ShotRepository;
import com.kidsanim.api.video.timeline.model.VideoTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TimelineServiceTest {

    private SceneRepository sceneRepository;
    private ShotRepository shotRepository;
    private AssetRepository assetRepository;
    private AudioCatalogService audioCatalogService;
    private KidsVideoProperties videoProperties;
    private ObjectMapper objectMapper;
    private TimelineService timelineService;

    @BeforeEach
    void setUp() {
        sceneRepository = Mockito.mock(SceneRepository.class);
        shotRepository = Mockito.mock(ShotRepository.class);
        assetRepository = Mockito.mock(AssetRepository.class);
        audioCatalogService = Mockito.mock(AudioCatalogService.class);
        videoProperties = new KidsVideoProperties();
        objectMapper = new ObjectMapper();

        timelineService = new TimelineService(
                sceneRepository, shotRepository, assetRepository,
                audioCatalogService, videoProperties, objectMapper
        );
    }

    @Test
    void buildTimelineCalculatesDurationsAndDuckingCorrectly() {
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "model.safetensors", "prompt", "neg");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 3", "Números", "Episodio Números");
        episode.setId(UUID.randomUUID());

        Scene scene1 = new Scene(episode, 1, null, com.kidsanim.api.domain.enums.ScenePurpose.INTRO, "playful");
        scene1.setId(UUID.randomUUID());

        Shot shot1 = new Shot(scene1, 1, "¡Hola amigos!", "Tito saludando");
        shot1.setId(UUID.randomUUID());
        shot1.setTargetDurationMs(4000);
        shot1.setOverlaysJson("""
                [{"type": "BIG_NUMBER", "value": "1", "position": "TOP_RIGHT", "appearAtWord": "Hola"}]
                """);
        shot1.setSfxJson("[\"pop\"]");

        Shot shot2 = new Shot(scene1, 2, "Vamos a contar", "Tito caminando");
        shot2.setId(UUID.randomUUID());
        shot2.setTargetDurationMs(5000);

        scene1.getShots().add(shot1);
        scene1.getShots().add(shot2);
        episode.getScenes().add(scene1);

        // Narration metadata
        Asset audioAsset = new Asset(episode, AssetType.NARRATION_AUDIO, "/storage/audio1.wav");
        audioAsset.setMetaJson("{\"durationMs\": 2500}");
        when(assetRepository.findByShotIdAndType(eq(shot1.getId()), eq(AssetType.NARRATION_AUDIO)))
                .thenReturn(Optional.of(audioAsset));

        when(audioCatalogService.resolveBgmPath(any())).thenReturn(java.nio.file.Path.of("./audio/bgm/upbeat_counting.mp3"));
        when(audioCatalogService.resolveSfxPath(eq("pop"))).thenReturn(java.nio.file.Path.of("./audio/sfx/pop.mp3"));

        VideoTimeline timeline = timelineService.buildTimeline(episode);

        assertThat(timeline).isNotNull();
        assertThat(timeline.totalDurationSeconds()).isEqualTo(9.0); // 4.0 + 5.0
        assertThat(timeline.entries()).hasSize(2);

        var entry1 = timeline.entries().get(0);
        assertThat(entry1.duration()).isEqualTo(4.0);
        assertThat(entry1.narrationDuration()).isEqualTo(2.5);
        assertThat(entry1.overlays()).hasSize(1);
        assertThat(entry1.sfxCues()).hasSize(1);

        var entry2 = timeline.entries().get(1);
        assertThat(entry2.startTime()).isEqualTo(4.0);
        assertThat(entry2.endTime()).isEqualTo(9.0);

        // Ducking points
        assertThat(timeline.audioMix().duckingPoints()).hasSize(1);
        assertThat(timeline.audioMix().duckingPoints().get(0).startTime()).isEqualTo(0.25);
        assertThat(timeline.audioMix().duckingPoints().get(0).endTime()).isEqualTo(2.75);
    }
}
