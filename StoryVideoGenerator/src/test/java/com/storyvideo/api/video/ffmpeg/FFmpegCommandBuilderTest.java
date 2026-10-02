package com.storyvideo.api.video.ffmpeg;

import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.TransitionType;
import com.storyvideo.api.video.timeline.model.AudioMixPlan;
import com.storyvideo.api.video.timeline.model.TimelineEntry;
import com.storyvideo.api.video.timeline.model.VideoTimeline;
import com.storyvideo.api.video.timeline.model.VolumeDuckPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FFmpegCommandBuilderTest {

    private FFmpegCommandBuilder commandBuilder;

    @BeforeEach
    void setUp() {
        commandBuilder = new FFmpegCommandBuilder("ffmpeg", 1080, 1920, 30, 22, "medium");
    }

    @Test
    @DisplayName("Construye comando FFmpeg completo con zoompan, concat y mezcla de audio")
    void shouldBuildCompleteRenderCommand() {
        UUID storyId = UUID.randomUUID();

        TimelineEntry entry1 = new TimelineEntry(
                UUID.randomUUID(), 1, 0.0, 5.0, 5.0,
                null, CameraMovement.SLOW_ZOOM_IN,
                TransitionType.FADE_IN, TransitionType.CUT, 0.4,
                List.of(), null, 0.35, 4.0, List.of(), List.of()
        );

        TimelineEntry entry2 = new TimelineEntry(
                UUID.randomUUID(), 2, 5.0, 11.0, 6.0,
                null, CameraMovement.KEN_BURNS,
                TransitionType.CUT, TransitionType.FADE_OUT, 0.4,
                List.of(), null, 0.35, 5.0, List.of(), List.of()
        );

        AudioMixPlan audioMix = new AudioMixPlan(
                null, 0.12,
                List.of(new VolumeDuckPoint(0.35, 4.35, 0.05)),
                List.of()
        );

        VideoTimeline timeline = new VideoTimeline(storyId, 11.0, List.of(entry1, entry2), audioMix);
        Path outputPath = Paths.get("target/output.mp4");

        List<String> command = commandBuilder.buildRenderCommand(timeline, outputPath);

        assertThat(command).isNotEmpty();
        assertThat(command.get(0)).isEqualTo("ffmpeg");
        assertThat(command).contains("-filter_complex");
        assertThat(command).contains("-c:v", "libx264");
        assertThat(command).contains("-c:a", "aac");
        assertThat(command).contains("-pix_fmt", "yuv420p");
        assertThat(command).contains(outputPath.toAbsolutePath().toString());

        // Verificar filter complex
        int filterIndex = command.indexOf("-filter_complex");
        String filterComplex = command.get(filterIndex + 1);

        assertThat(filterComplex).contains("scale=2160:3840");
        assertThat(filterComplex).contains("zoompan");
        assertThat(filterComplex).contains("concat=n=2:v=1:a=0[vout]");
        assertThat(filterComplex).contains("aevalsrc=0:d=11.00");
        assertThat(filterComplex).contains("amix=inputs=1");
        assertThat(filterComplex).contains("alimiter=limit=0.95[aout]");
    }

    @Test
    @DisplayName("Soporta todos los tipos de CameraMovement sin error")
    void shouldSupportAllCameraMovements() {
        for (CameraMovement movement : CameraMovement.values()) {
            TimelineEntry entry = new TimelineEntry(
                    UUID.randomUUID(), 1, 0.0, 3.0, 3.0,
                    null, movement,
                    TransitionType.CUT, TransitionType.CUT, 0.0,
                    List.of(), null, 0.0, 0.0, List.of(), List.of()
            );

            VideoTimeline timeline = new VideoTimeline(UUID.randomUUID(), 3.0, List.of(entry), new AudioMixPlan(null, 0.12, List.of(), List.of()));
            List<String> command = commandBuilder.buildRenderCommand(timeline, Paths.get("target/test.mp4"));

            int filterIndex = command.indexOf("-filter_complex");
            String filterComplex = command.get(filterIndex + 1);
            assertThat(filterComplex).contains("zoompan=");
        }
    }

    @Test
    @DisplayName("Lanza excepción si el timeline no tiene entradas")
    void shouldThrowExceptionWhenEmptyTimeline() {
        VideoTimeline empty = new VideoTimeline(UUID.randomUUID(), 0.0, List.of(), null);
        assertThatThrownBy(() -> commandBuilder.buildRenderCommand(empty, Paths.get("target/test.mp4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }
}
