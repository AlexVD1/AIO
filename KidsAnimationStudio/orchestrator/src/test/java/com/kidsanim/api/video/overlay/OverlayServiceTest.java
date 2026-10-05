package com.kidsanim.api.video.overlay;

import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.script.model.OverlaySpec;
import com.kidsanim.api.video.timeline.model.TimelineEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OverlayServiceTest {

    private final OverlayService overlayService = new OverlayService();

    @Test
    void generateOverlayStylesReturnsExpectedStyles() {
        List<String> styles = overlayService.generateOverlayStyles(1920, 1080);
        assertThat(styles).hasSize(2);
        assertThat(styles.get(0)).contains("Style: KidsOverlayBig");
        assertThat(styles.get(1)).contains("Style: KidsOverlayBadge");
    }

    @Test
    void generateOverlayEventsFormatsBigNumberWithSyncAndPopIn() {
        OverlaySpec overlay = new OverlaySpec("BIG_NUMBER", "3", "TOP_RIGHT", "tres");
        TimelineEntry entry = new TimelineEntry(
                UUID.randomUUID(), 1, 1, 1,
                0.0, 4.0, 4.0,
                "/path/clip.mp4", null,
                CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME,
                "¡Mira, hay tres manzanas!", "/path/audio.wav",
                0.25, 3.0,
                List.of(overlay), List.of()
        );

        List<OverlayService.WordTiming> words = List.of(
                new OverlayService.WordTiming("mira", 0.1, 0.4),
                new OverlayService.WordTiming("hay", 0.5, 0.8),
                new OverlayService.WordTiming("tres", 0.9, 1.3),
                new OverlayService.WordTiming("manzanas", 1.4, 2.2)
        );

        List<String> events = overlayService.generateOverlayEvents(entry, words, 1920, 1080);
        assertThat(events).hasSize(1);

        String event = events.get(0);
        assertThat(event).startsWith("Dialogue: 1,");
        assertThat(event).contains("KidsOverlayBig");
        assertThat(event).contains("3");
        // Pop in animation
        assertThat(event).contains("\\fad(150,200)");
        assertThat(event).contains("\\fscx130\\fscy130");
        // Positioned around TOP_RIGHT
        assertThat(event).contains("\\pos(");
        // Start time should be: entry.startTime(0.0) + narrationOffset(0.25) + word.start(0.9) = 1.15s -> "0:00:01.15"
        assertThat(event).contains("0:00:01.15");
    }

    @Test
    void generateOverlayEventsFormatsShapeAndColor() {
        OverlaySpec shape = new OverlaySpec("SHAPE", "CÍRCULO", "CENTER", null);
        OverlaySpec color = new OverlaySpec("COLOR_SWATCH", "ROJO", "BOTTOM_CENTER", null);
        TimelineEntry entry = new TimelineEntry(
                UUID.randomUUID(), 1, 1, 1,
                2.0, 6.0, 4.0,
                null, "/path/img.png",
                CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME,
                "Un círculo rojo", null,
                0.0, 0.0,
                List.of(shape, color), List.of()
        );

        List<String> events = overlayService.generateOverlayEvents(entry, List.of(), 1920, 1080);
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).contains("● CÍRCULO");
        assertThat(events.get(1)).contains("ROJO");
    }
}
