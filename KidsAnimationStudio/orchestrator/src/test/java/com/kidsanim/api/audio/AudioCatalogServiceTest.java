package com.kidsanim.api.audio;

import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AudioCatalogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveBgmFileNameReturnsExpectedTrack() {
        KidsVideoProperties properties = new KidsVideoProperties(
                "ffmpeg", "ffprobe", 1920, 1080, 30,
                "h264_nvenc", "libx264", "medium", 22,
                900L, tempDir.toString(), 0.12, "./export", "Arial"
        );
        AudioCatalogService service = new AudioCatalogService(properties);

        assertThat(service.resolveBgmFileName(EducationalTopicType.COUNTING)).isEqualTo("upbeat_counting.mp3");
        assertThat(service.resolveBgmFileName(EducationalTopicType.COLORS)).isEqualTo("playful_colors.mp3");
        assertThat(service.resolveBgmFileName(EducationalTopicType.DAILY_ROUTINES)).isEqualTo("calm_routine.mp3");
        assertThat(service.resolveBgmFileName(null)).isEqualTo("kids_playful_theme.mp3");

        Path bgmPath = service.resolveBgmPath(EducationalTopicType.COUNTING);
        assertThat(bgmPath.toString()).contains("upbeat_counting.mp3");
    }

    @Test
    void resolveSfxPathReturnsExpectedFile() {
        KidsVideoProperties properties = new KidsVideoProperties(
                "ffmpeg", "ffprobe", 1920, 1080, 30,
                "h264_nvenc", "libx264", "medium", 22,
                900L, tempDir.toString(), 0.12, "./export", "Arial"
        );
        AudioCatalogService service = new AudioCatalogService(properties);

        Path popPath = service.resolveSfxPath("pop");
        assertThat(popPath.toString()).endsWith("pop.mp3");

        Path chimePath = service.resolveSfxPath("CHIME");
        assertThat(chimePath.toString()).endsWith("chime.mp3");
    }
}
