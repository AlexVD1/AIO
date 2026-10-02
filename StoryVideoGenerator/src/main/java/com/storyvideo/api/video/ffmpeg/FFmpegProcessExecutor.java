package com.storyvideo.api.video.ffmpeg;

import com.storyvideo.api.video.ffmpeg.exception.FFmpegExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Component
public class FFmpegProcessExecutor {

    private static final Logger log = LoggerFactory.getLogger(FFmpegProcessExecutor.class);

    private final long timeoutSeconds;

    public record ExecutionResult(int exitCode, Duration duration, List<String> logTail) {}

    public FFmpegProcessExecutor(
            @Value("${story.video.render-timeout-seconds:600}") long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public ExecutionResult execute(List<String> command) {
        Instant startTime = Instant.now();
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Redirigir stderr a stdout para unificar lectura

        Process process;
        try {
            log.info("Iniciando proceso FFmpeg (timeout: {}s)...", timeoutSeconds);
            process = processBuilder.start();
        } catch (Exception e) {
            throw new FFmpegExecutionException("No se pudo iniciar el proceso de FFmpeg", e);
        }

        List<String> outputLines = Collections.synchronizedList(new ArrayList<>());
        ExecutorService readerExecutor = Executors.newSingleThreadExecutor();

        Future<?> readerFuture = readerExecutor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                    if (outputLines.size() > 500) {
                        outputLines.remove(0); // Mantener últimas 500 líneas en memoria
                    }
                    if (line.contains("time=") || line.contains("fps=")) {
                        log.debug("FFmpeg progreso: {}", line.trim());
                    }
                }
            } catch (Exception ignored) {
                // Stream cerrado
            }
        });

        boolean finished;
        int exitCode = -1;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("Proceso FFmpeg excedió el tiempo límite de {}s. Forzando terminación...", timeoutSeconds);
                process.destroyForcibly();
                throw new FFmpegExecutionException("El renderizado excedió el tiempo límite de " + timeoutSeconds + "s", -1, getTail(outputLines, 30));
            }
            exitCode = process.exitValue();
            readerFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new FFmpegExecutionException("El proceso FFmpeg fue interrumpido", e);
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Lector de salida de FFmpeg finalizó con timeout o error leve");
        } finally {
            readerExecutor.shutdownNow();
        }

        Duration duration = Duration.between(startTime, Instant.now());

        if (exitCode != 0) {
            String tail = getTail(outputLines, 30);
            log.error("FFmpeg falló con exit code {}. Últimas líneas:\n{}", exitCode, tail);
            throw new FFmpegExecutionException("FFmpeg finalizó con código de error " + exitCode, exitCode, tail);
        }

        log.info("Proceso FFmpeg completado exitosamente en {} ms (exit code: 0)", duration.toMillis());
        return new ExecutionResult(exitCode, duration, getTailList(outputLines, 20));
    }

    private String getTail(List<String> lines, int maxLines) {
        return String.join("\n", getTailList(lines, maxLines));
    }

    private List<String> getTailList(List<String> lines, int maxLines) {
        synchronized (lines) {
            int size = lines.size();
            int start = Math.max(0, size - maxLines);
            return new ArrayList<>(lines.subList(start, size));
        }
    }
}
