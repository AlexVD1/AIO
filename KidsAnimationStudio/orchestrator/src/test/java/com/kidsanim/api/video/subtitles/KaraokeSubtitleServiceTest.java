package com.kidsanim.api.video.subtitles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.script.model.OverlaySpec;
import com.kidsanim.api.video.overlay.OverlayService;
import com.kidsanim.api.video.timeline.model.AudioMixPlan;
import com.kidsanim.api.video.timeline.model.TimelineEntry;
import com.kidsanim.api.video.timeline.model.VideoTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class KaraokeSubtitleServiceTest {

    private AssetRepository assetRepository;
    private OverlayService overlayService;
    private KidsVideoProperties videoProperties;
    private ObjectMapper objectMapper;
    private KaraokeSubtitleService karaokeSubtitleService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        assetRepository = Mockito.mock(AssetRepository.class);
        overlayService = new OverlayService();
        videoProperties = new KidsVideoProperties();
        objectMapper = new ObjectMapper();
        karaokeSubtitleService = new KaraokeSubtitleService(assetRepository, overlayService, videoProperties, objectMapper);
    }

    @Test
    void writeKaraokeAssFileGeneratesValidAssWithKaraokeTagsAndOverlays() throws Exception {
        UUID episodeId = UUID.randomUUID();
        UUID shotId = UUID.randomUUID();

        // Mock del asset WORD_TIMESTAMPS
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "Aprender", "Episodio 1");

        Asset timestampAsset = new Asset(episode, AssetType.WORD_TIMESTAMPS, "/storage/timestamps.json");
        timestampAsset.setMetaJson("""
                {
                  "words": [
                    {"word": "Uno", "startMs": 100, "endMs": 500},
                    {"word": "dos", "startMs": 600, "endMs": 1000},
                    {"word": "tres", "startMs": 1100, "endMs": 1600}
                  ]
                }
                """);

        when(assetRepository.findByShotIdAndType(eq(shotId), eq(AssetType.WORD_TIMESTAMPS)))
                .thenReturn(Optional.of(timestampAsset));

        OverlaySpec overlay = new OverlaySpec("BIG_NUMBER", "3", "TOP_RIGHT", "tres");
        TimelineEntry entry = new TimelineEntry(
                shotId, 1, 1, 1,
                0.0, 4.0, 4.0,
                "/path/clip.mp4", null,
                CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME,
                "Uno dos tres", "/path/narration.wav",
                0.25, 2.0,
                List.of(overlay), List.of()
        );

        VideoTimeline timeline = new VideoTimeline(
                episodeId, 4.0, 1920, 1080, 30,
                List.of(entry), new AudioMixPlan(null, 0.12, List.of(), List.of())
        );

        Path targetFile = tempDir.resolve("karaoke.ass");
        Path result = karaokeSubtitleService.writeKaraokeAssFile(timeline, targetFile);

        assertThat(Files.exists(result)).isTrue();
        String content = Files.readString(result);

        assertThat(content).contains("[Script Info]");
        assertThat(content).contains("PlayResX: 1920");
        assertThat(content).contains("PlayResY: 1080");
        assertThat(content).contains("Style: KidsKaraoke");
        assertThat(content).contains("Style: KidsOverlayBig");
        assertThat(content).contains("{\\k");
        assertThat(content).contains("Uno");
        assertThat(content).contains("dos");
        assertThat(content).contains("tres");
        // Verifica que se generó el evento de overlay
        assertThat(content).contains("KidsOverlayBig");
        assertThat(content).contains("{\\c&H00D7FF&}3");
    }
}
