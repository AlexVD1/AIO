package com.kidsanim.api.video.ffmpeg;

import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.video.timeline.model.AudioMixPlan;
import com.kidsanim.api.video.timeline.model.TimelineEntry;
import com.kidsanim.api.video.timeline.model.VideoTimeline;
import com.kidsanim.api.video.timeline.model.VolumeDuckPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FFmpegCommandBuilderTest {

    private FFmpegCommandBuilder commandBuilder;
    private KidsVideoProperties videoProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        videoProperties = new KidsVideoProperties(
                "ffmpeg", "ffprobe", 1920, 1080, 30,
                "h264_nvenc", "libx264", "medium", 22,
                900L, "./audio", 0.12, "./export", "Arial"
        );
        commandBuilder = new FFmpegCommandBuilder(videoProperties);
    }

    @Test
    void buildRenderCommandIncludesExpectedFiltersAndLoudnorm() throws Exception {
        UUID episodeId = UUID.randomUUID();
        Path clipFile1 = tempDir.resolve("shot1.mp4");
        Path clipFile2 = tempDir.resolve("shot2.mp4");
        Path bgmFile = tempDir.resolve("bgm.mp3");
        Path narrationFile1 = tempDir.resolve("narr1.wav");
        Path assFile = tempDir.resolve("subtitles.ass");
        Path outputFile = tempDir.resolve("final.mp4");

        Files.createFile(clipFile1);
        Files.createFile(clipFile2);
        Files.createFile(bgmFile);
        Files.createFile(narrationFile1);
        Files.createFile(assFile);

        TimelineEntry entry1 = new TimelineEntry(
                UUID.randomUUID(), 1, 1, 1,
                0.0, 4.0, 4.0,
                clipFile1.toString(), null,
                CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME,
                "Hola niños", narrationFile1.toString(),
                0.25, 2.5,
                List.of(), List.of()
        );

        TimelineEntry entry2 = new TimelineEntry(
                UUID.randomUUID(), 2, 2, 1, // Escena 2 (genera transición suave)
                4.0, 8.0, 4.0,
                clipFile2.toString(), null,
                CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME,
                "Vamos a contar", null,
                0.0, 0.0,
                List.of(), List.of()
        );

        AudioMixPlan audioMix = new AudioMixPlan(
                bgmFile.toString(),
                0.12,
                List.of(new VolumeDuckPoint(0.25, 2.75, 0.05)),
                List.of()
        );

        VideoTimeline timeline = new VideoTimeline(
                episodeId, 8.0, 1920, 1080, 30,
                List.of(entry1, entry2), audioMix
        );

        List<String> command = commandBuilder.buildRenderCommand(timeline, outputFile, assFile);

        String fullCommand = String.join(" ", command);

        assertThat(command.get(0)).isEqualTo("ffmpeg");
        assertThat(fullCommand).contains("-hide_banner");
        assertThat(fullCommand).contains("-y");
        assertThat(fullCommand).contains("scale=1920:1080");
        assertThat(fullCommand).contains("concat=n=2:v=1:a=0");
        assertThat(fullCommand).contains("ass=");
        assertThat(fullCommand).contains("loudnorm=I=-14:LRA=11:TP=-1.5");
        assertThat(fullCommand).contains("h264_nvenc");
        assertThat(fullCommand).contains("-c:a aac");
        assertThat(fullCommand).contains("-movflags +faststart");
        assertThat(fullCommand).contains(outputFile.toAbsolutePath().toString());
    }

    @Test
    void buildRenderCommandSupportsFallbackEncoder() throws Exception {
        UUID episodeId = UUID.randomUUID();
        Path clipFile = tempDir.resolve("single.mp4");
        Files.createFile(clipFile);

        TimelineEntry entry = new TimelineEntry(
                UUID.randomUUID(), 1, 1, 1,
                0.0, 3.0, 3.0,
                clipFile.toString(), null,
                CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME,
                "Texto", null, 0.0, 0.0, List.of(), List.of()
        );

        VideoTimeline timeline = new VideoTimeline(
                episodeId, 3.0, 1920, 1080, 30,
                List.of(entry), new AudioMixPlan(null, 0.12, List.of(), List.of())
        );

        Path outputFile = tempDir.resolve("out.mp4");
        List<String> command = commandBuilder.buildRenderCommandWithEncoder(timeline, outputFile, null, "libx264");

        String fullCommand = String.join(" ", command);
        assertThat(fullCommand).contains("-c:v libx264");
        assertThat(fullCommand).contains("-crf 22");
    }
}
