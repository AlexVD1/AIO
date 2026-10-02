package com.storyvideo.api.video.subtitles.service;

import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryScene;
import com.storyvideo.api.video.timeline.model.AudioMixPlan;
import com.storyvideo.api.video.timeline.model.TimelineEntry;
import com.storyvideo.api.video.timeline.model.VideoTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubtitleServiceTest {

    private SubtitleService subtitleService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        subtitleService = new SubtitleService();
    }

    @Test
    @DisplayName("Genera entradas de subtítulos dividiendo el texto de narración en fragmentos")
    void shouldBuildSubtitleEntries() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia Subtitulada", StoryGenre.HORROR, "Miedo");
        story.setId(storyId);

        StoryScene scene = new StoryScene(1, "En una noche fría y oscura sin luna, un susurro cruzó la habitación.");
        story.addScene(scene);

        TimelineEntry entry = new TimelineEntry(
                UUID.randomUUID(), 1, 0.0, 6.0, 6.0,
                null, null, null, null, 0.0,
                List.of(), null, 0.35, 5.0, List.of(), List.of()
        );

        VideoTimeline timeline = new VideoTimeline(storyId, 6.0, List.of(entry), new AudioMixPlan(null, 0.12, List.of(), List.of()));

        List<SubtitleService.SubtitleEntry> entries = subtitleService.buildSubtitleEntries(story, timeline);

        assertThat(entries).isNotEmpty();
        assertThat(entries.get(0).index()).isEqualTo(1);
        assertThat(entries.get(0).startSeconds()).isGreaterThanOrEqualTo(0.35);
        assertThat(entries.get(0).text()).isNotBlank();
    }

    @Test
    @DisplayName("Escribe archivo .srt con formato y marcas de tiempo válidas")
    void shouldWriteSrtFile() throws IOException {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia", StoryGenre.MYSTERY, "Investigacion");
        story.setId(storyId);

        StoryScene scene = new StoryScene(1, "La pista estaba oculta tras el cuadro antiguo.");
        story.addScene(scene);

        TimelineEntry entry = new TimelineEntry(
                UUID.randomUUID(), 1, 0.0, 4.0, 4.0,
                null, null, null, null, 0.0,
                List.of(), null, 0.25, 3.5, List.of(), List.of()
        );

        VideoTimeline timeline = new VideoTimeline(storyId, 4.0, List.of(entry), null);
        Path targetPath = tempDir.resolve("story.srt");

        Path generated = subtitleService.writeSrtFile(story, timeline, targetPath);

        assertThat(Files.exists(generated)).isTrue();
        String content = Files.readString(generated);
        assertThat(content).contains("00:00:");
        assertThat(content).contains("-->");
        assertThat(content).contains("La pista");
    }
}
