package com.storyvideo.api.video.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.video.validation.dto.VideoValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class QualityValidationServiceTest {

    private QualityValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new QualityValidationService("ffprobe", new ObjectMapper());
    }

    @Test
    @DisplayName("Reporta error si el archivo de video no existe")
    void shouldReportErrorWhenFileDoesNotExist() {
        Path nonExistent = Paths.get("target/non_existent_video.mp4");
        VideoValidationResult result = validationService.validate(nonExistent, 10.0);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).anyMatch(issue -> issue.contains("no existe"));
    }

    @Test
    @DisplayName("Valida un video vertical real generado con FFmpeg")
    void shouldValidateRealFfmpegVideo() throws Exception {
        Path testDir = Paths.get("target/test-video-validation");
        Files.createDirectories(testDir);
        Path testMp4 = testDir.resolve("sample_1080x1920.mp4");

        // Generar un micro video de prueba de 1 segundo (1080x1920, h264, aac)
        Process process = new ProcessBuilder(
                "ffmpeg", "-y", "-hide_banner",
                "-f", "lavfi", "-i", "color=c=black:s=1080x1920:d=1.0:r=30",
                "-f", "lavfi", "-i", "aevalsrc=0:d=1.0:s=44100:c=stereo",
                "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "128k",
                testMp4.toAbsolutePath().toString()
        ).start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // Si por alguna razón de entorno no pudo ejecutarse, saltar aserción
            return;
        }

        VideoValidationResult result = validationService.validate(testMp4, 1.0);

        assertThat(result.valid()).isTrue();
        assertThat(result.width()).isEqualTo(1080);
        assertThat(result.height()).isEqualTo(1920);
        assertThat(result.videoCodec()).isEqualToIgnoringCase("h264");
        assertThat(result.audioCodec()).isEqualToIgnoringCase("aac");
        assertThat(result.actualDuration()).isBetween(0.8, 1.2);
    }
}
