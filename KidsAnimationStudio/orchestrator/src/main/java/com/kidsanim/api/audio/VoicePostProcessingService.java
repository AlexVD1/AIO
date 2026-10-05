package com.kidsanim.api.audio;

import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Servicio de post-procesamiento acústico de voz infantil (Fase Q2.6).
 * Aplica:
 * 1. Filtro paso-altos (HPF 80 Hz) para eliminar retumbos y ruido de fondo subgrave.
 * 2. Compresor de emisión suave (compand) para atenuar picos agresivos y sibilancias.
 * 3. Normalización EBU R128 a -16 LUFS (estándar de diálogo antes de mezcla final).
 */
@Service
public class VoicePostProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VoicePostProcessingService.class);

    private final String ffmpegPath;

    public VoicePostProcessingService(KidsVideoProperties videoProperties) {
        this.ffmpegPath = videoProperties != null && videoProperties.ffmpegPath() != null
                ? videoProperties.ffmpegPath()
                : "ffmpeg";
    }

    public Path processVoiceAudio(Path inputAudioPath, Path outputAudioPath) {
        if (inputAudioPath == null || !Files.exists(inputAudioPath)) {
            log.warn("El archivo de audio de entrada no existe en disco para post-proceso: {}. Saltando.", inputAudioPath);
            return inputAudioPath;
        }

        Path target = outputAudioPath != null ? outputAudioPath : inputAudioPath;
        Path tempOutput = null;

        try {
            tempOutput = Files.createTempFile("voice_processed_", ".wav");
            
            // Cadena de filtros FFmpeg para voz:
            // highpass=f=80 -> elimina rumble
            // compand -> compresión y control dinámico
            // loudnorm=I=-16:TP=-1.5:LRA=11 -> normalización de pista de voz a -16 LUFS
            String audioFilter = "highpass=f=80," +
                    "compand=attacks=0.03:decays=0.15:points=-80/-80|-40/-30|-20/-16|0/-12," +
                    "loudnorm=I=-16:TP=-1.5:LRA=11";

            List<String> command = List.of(
                    ffmpegPath,
                    "-hide_banner",
                    "-loglevel", "error",
                    "-y",
                    "-i", inputAudioPath.toAbsolutePath().toString(),
                    "-af", audioFilter,
                    "-ar", "24000",
                    tempOutput.toAbsolutePath().toString()
            );

            log.info("Aplicando post-proceso de voz FFmpeg a '{}'...", inputAudioPath.getFileName());
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(tempOutput) && Files.size(tempOutput) > 0) {
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.move(tempOutput, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Audio procesado exitosamente guardado en '{}'", target);
                return target;
            } else {
                log.warn("FFmpeg falló al procesar voz (código {}). Se conserva el audio original.", exitCode);
            }
        } catch (Exception e) {
            log.error("Error durante el post-proceso de voz con FFmpeg: {}", e.getMessage(), e);
        } finally {
            if (tempOutput != null && Files.exists(tempOutput)) {
                try {
                    Files.deleteIfExists(tempOutput);
                } catch (IOException ignored) {
                }
            }
        }

        // Fallback: si falla el procesamiento, retorna la ruta de entrada
        return inputAudioPath;
    }
}
