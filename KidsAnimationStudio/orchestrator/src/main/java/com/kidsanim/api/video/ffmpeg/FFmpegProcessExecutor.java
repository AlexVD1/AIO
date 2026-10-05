package com.kidsanim.api.video.ffmpeg;

import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public FFmpegProcessExecutor(KidsVideoProperties videoProperties) {
        this.timeoutSeconds = videoProperties != null && videoProperties.renderTimeoutSeconds() != null
                ? videoProperties.renderTimeoutSeconds()
                : 900L;
    }

    public ExecutionResult execute(List<String> command) {
        Instant startTime = Instant.now();
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Unificar stderr en stdout para prevenir bloqueos de pipe

        Process process;
        try {
            log.info("Iniciando proceso FFmpeg con {} argumentos (timeout: {}s)...", command.size(), timeoutSeconds);
            process = processBuilder.start();
        } catch (Exception e) {
            throw new FFmpegExecutionException("No se pudo iniciar el subproceso de FFmpeg", e);
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
                        outputLines.remove(0);
                    }
                    if (line.contains("time=") || line.contains("fps=")) {
                        log.debug("FFmpeg progreso: {}", line.trim());
                    }
                }
            } catch (Exception ignored) {
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
            log.warn("Lector de salida de FFmpeg finalizó con timeout leve");
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
