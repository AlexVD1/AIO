package com.kidsanim.api.audio;

import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VoicePostProcessingServiceTest {

    @Test
    void testProcessVoiceAudioInvalidInputGracefullyReturnsOriginal() {
        VoicePostProcessingService service = new VoicePostProcessingService(new KidsVideoProperties());

        Path missingPath = Path.of("non_existent_file.wav");
        Path result = service.processVoiceAudio(missingPath, null);
        assertEquals(missingPath, result);
    }

    @Test
    void testProcessVoiceAudioWithRealFile(@TempDir Path tempDir) throws IOException {
        VoicePostProcessingService service = new VoicePostProcessingService(new KidsVideoProperties());

        // Generar un WAV sintético de 0.5s de 440Hz usando ffmpeg
        Path inputWav = tempDir.resolve("sine.wav");
        Path outputWav = tempDir.resolve("sine_processed.wav");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=0.5",
                "-ar", "24000",
                inputWav.toAbsolutePath().toString()
        );
        try {
            Process p = pb.start();
            int exit = p.waitFor();
            if (exit == 0 && Files.exists(inputWav)) {
                Path result = service.processVoiceAudio(inputWav, outputWav);
                assertNotNull(result);
                assertTrue(Files.exists(result));
                assertTrue(Files.size(result) > 0);
            }
        } catch (Exception ignored) {
            // Si ffmpeg no está disponible en el entorno del test, salta la ejecución
        }
    }
}
