package com.kidsanim.api.video.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.video.validation.dto.VideoValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class QualityValidationService {

    private static final Logger log = LoggerFactory.getLogger(QualityValidationService.class);

    private final String ffprobePath;
    private final ObjectMapper objectMapper;

    public QualityValidationService(KidsVideoProperties videoProperties, ObjectMapper objectMapper) {
        this.ffprobePath = videoProperties != null && videoProperties.ffprobePath() != null
                ? videoProperties.ffprobePath()
                : "ffprobe";
        this.objectMapper = objectMapper;
    }

    public VideoValidationResult validate(Path videoPath, double expectedDurationSeconds, int targetWidth, int targetHeight) {
        List<String> issues = new ArrayList<>();

        if (videoPath == null || !Files.exists(videoPath)) {
            issues.add("El archivo de video no existe en el disco");
            return new VideoValidationResult(false, issues, 0.0, null, null, 0, 0, 0, null);
        }

        long fileSizeBytes = 0;
        try {
            fileSizeBytes = Files.size(videoPath);
            if (fileSizeBytes < 1024) {
                issues.add("El tamaño del archivo de video es demasiado pequeño: " + fileSizeBytes + " bytes");
            }
        } catch (Exception e) {
            issues.add("No se pudo obtener el tamaño del archivo: " + e.getMessage());
        }

        List<String> probeCmd = List.of(
                ffprobePath,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                videoPath.toAbsolutePath().toString()
        );

        String jsonOutput;
        try {
            Process process = new ProcessBuilder(probeCmd).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                issues.add("ffprobe excedió el tiempo límite de inspección (30s)");
                return new VideoValidationResult(false, issues, 0.0, null, null, 0, 0, fileSizeBytes, null);
            }

            if (process.exitValue() != 0) {
                issues.add("ffprobe finalizó con código de error " + process.exitValue());
                return new VideoValidationResult(false, issues, 0.0, null, null, 0, 0, fileSizeBytes, null);
            }

            jsonOutput = sb.toString();
        } catch (Exception e) {
            issues.add("Error al invocar ffprobe: " + e.getMessage());
            return new VideoValidationResult(false, issues, 0.0, null, null, 0, 0, fileSizeBytes, null);
        }

        double actualDuration = 0.0;
        String videoCodec = null;
        String audioCodec = null;
        int width = 0;
        int height = 0;

        try {
            JsonNode root = objectMapper.readTree(jsonOutput);
            JsonNode format = root.path("format");
            if (format.has("duration")) {
                actualDuration = format.path("duration").asDouble(0.0);
            }

            JsonNode streams = root.path("streams");
            if (streams.isArray()) {
                for (JsonNode stream : streams) {
                    String codecType = stream.path("codec_type").asText();
                    if ("video".equalsIgnoreCase(codecType)) {
                        videoCodec = stream.path("codec_name").asText();
                        width = stream.path("width").asInt();
                        height = stream.path("height").asInt();
                    } else if ("audio".equalsIgnoreCase(codecType)) {
                        audioCodec = stream.path("codec_name").asText();
                    }
                }
            }
        } catch (Exception e) {
            issues.add("Error al parsear la salida JSON de ffprobe: " + e.getMessage());
            return new VideoValidationResult(false, issues, 0.0, null, null, 0, 0, fileSizeBytes, jsonOutput);
        }

        // Reglas de validación
        if (videoCodec == null) {
            issues.add("No se detectó ningún stream de video en el archivo MP4");
        } else if (!"h264".equalsIgnoreCase(videoCodec) && !"hevc".equalsIgnoreCase(videoCodec)) {
            issues.add("Codec de video no estándar: " + videoCodec + " (esperado: h264)");
        }

        int expectedW = targetWidth > 0 ? targetWidth : 1920;
        int expectedH = targetHeight > 0 ? targetHeight : 1080;
        if (width != expectedW || height != expectedH) {
            issues.add(String.format("Resolución no conforme: %dx%d (esperado: %dx%d)", width, height, expectedW, expectedH));
        }

        if (audioCodec == null) {
            issues.add("No se detectó ningún stream de audio en el archivo MP4");
        } else if (!"aac".equalsIgnoreCase(audioCodec) && !"mp3".equalsIgnoreCase(audioCodec)) {
            issues.add("Codec de audio no estándar: " + audioCodec + " (esperado: aac)");
        }

        if (expectedDurationSeconds > 0 && Math.abs(actualDuration - expectedDurationSeconds) > 3.0) {
            issues.add(String.format("Duración fuera de tolerancia: %.2fs (esperado: %.2fs ±3.0s)",
                    actualDuration, expectedDurationSeconds));
        }

        boolean valid = issues.isEmpty();
        if (valid) {
            log.info("Validación de video exitosa: {} ({}x{}, {}s, {} bytes)",
                    videoPath.getFileName(), width, height, actualDuration, fileSizeBytes);
        } else {
            log.warn("Video {} no superó la validación con {} incidencias: {}",
                    videoPath.getFileName(), issues.size(), issues);
        }

        return new VideoValidationResult(
                valid,
                issues,
                actualDuration,
                videoCodec,
                audioCodec,
                width,
                height,
                fileSizeBytes,
                jsonOutput
        );
    }
}
