package com.trivia.api.service.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ejecutor de procesos del sistema operativo especializado en FFmpeg.
 * Gestiona timeouts, captura de streams de logs y validación de códigos de retorno.
 */
@Component
public class FFmpegProcessExecutor {

    private static final Logger log = LoggerFactory.getLogger(FFmpegProcessExecutor.class);

    private final String ffmpegBinary;
    private final long timeoutSeconds;

    public FFmpegProcessExecutor(
            @Value("${trivia.video.ffmpeg-path:ffmpeg}") String ffmpegBinary,
            @Value("${trivia.video.timeout-seconds:300}") long timeoutSeconds) {
        this.ffmpegBinary = ffmpegBinary;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getFfmpegBinary() {
        return ffmpegBinary;
    }

    /**
     * Ejecuta una invocación de FFmpeg.
     *
     * @param args Argumentos que se pasarán a FFmpeg (sin incluir el ejecutable 'ffmpeg').
     * @param workingDir Directorio de trabajo opcional.
     */
    public void execute(List<String> args, File workingDir) {
        List<String> fullCommand = new java.util.ArrayList<>();
        fullCommand.add(ffmpegBinary);
        fullCommand.addAll(args);

        log.debug("Ejecutando comando: {}", String.join(" ", fullCommand));

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.trace("[FFmpeg] {}", line);
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("El proceso de FFmpeg excedió el tiempo límite de " + timeoutSeconds + "s");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg finalizó con código de error {}. Salida:\n{}", exitCode, output);
                throw new IllegalStateException("Error al ejecutar FFmpeg (exit code " + exitCode + "): " + output);
            }

        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Error al invocar proceso de FFmpeg: " + e.getMessage(), e);
        }
    }
}
