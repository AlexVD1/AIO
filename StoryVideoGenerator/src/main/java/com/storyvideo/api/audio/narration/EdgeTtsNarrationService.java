package com.storyvideo.api.audio.narration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.storyvideo.api.audio.narration.dto.NarrationRequest;
import com.storyvideo.api.audio.narration.dto.NarrationResult;
import com.storyvideo.api.audio.narration.exception.NarrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service("edgeTtsNarrationService")
@Primary
public class EdgeTtsNarrationService implements NarrationService {

    private static final Logger log = LoggerFactory.getLogger(EdgeTtsNarrationService.class);

    private final String pythonExecutable;
    private final ObjectMapper objectMapper;
    private final Path scriptPath;

    public EdgeTtsNarrationService(
            @Value("${story.video.python-path:python}") String configuredPython,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.pythonExecutable = resolvePythonExecutable(configuredPython);
        this.scriptPath = resolveScriptPath();
        log.info("EdgeTtsNarrationService inicializado (python: '{}', script: '{}')", pythonExecutable, scriptPath);
    }

    @Override
    public String getProviderName() {
        return "Edge-TTS (Microsoft Neural Voices via Python bridge)";
    }

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder(pythonExecutable, "-c", "import edge_tts; print('OK')")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(4, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Edge-TTS no disponible en host: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public NarrationResult synthesize(NarrationRequest request) {
        log.info("Sintetizando voz para escena {} con voz '{}' ({} caracteres)",
                request.sequenceNumber(), request.resolvedVoice(), request.text().length());

        Path tempDir = Paths.get("temp").toAbsolutePath().normalize();
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            throw new NarrationException("No se pudo crear directorio temporal: " + tempDir, e);
        }

        String fileName = String.format("tts_%s_%02d_%s.mp3",
                request.storyId() != null ? request.storyId() : "temp",
                request.sequenceNumber(),
                UUID.randomUUID().toString().substring(0, 8));
        Path tempOutputFile = tempDir.resolve(fileName);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("text", request.text());
        payload.put("voice", request.resolvedVoice());
        payload.put("output_path", tempOutputFile.toString().replace('\\', '/'));
        payload.put("rate", request.resolvedRate());
        payload.put("pitch", request.resolvedPitch());

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, scriptPath.toString());
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONLEGACYWINDOWSSTDIO", "utf-8");
            pb.redirectErrorStream(false);
            process = pb.start();

            // Enviar payload JSON a stdin
            try (OutputStream os = process.getOutputStream();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                writer.write(payload.toString());
                writer.flush();
            }

            // Capturar stdout y stderr
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            boolean completed = process.waitFor(45, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new NarrationException("Timeout esperando respuesta de síntesis TTS de Edge-TTS (45s)");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Fallo en tts_bridge.py (exit code: {}). Stderr: {}, Stdout: {}", exitCode, stderr, stdout);
                throw new NarrationException("Fallo en ejecución de script de síntesis TTS: " + stderr);
            }

            JsonNode root = objectMapper.readTree(stdout);
            if (!root.path("success").asBoolean(false)) {
                String error = root.path("error").asText("Error desconocido en bridge de TTS");
                throw new NarrationException("Edge-TTS reportó error: " + error);
            }

            double durationSeconds = root.path("duration_seconds").asDouble(0.0);
            String voiceUsed = root.path("voice").asText(request.resolvedVoice());

            if (!Files.exists(tempOutputFile)) {
                throw new NarrationException("El archivo de audio sintetizado no fue encontrado en: " + tempOutputFile);
            }

            byte[] audioBytes = Files.readAllBytes(tempOutputFile);
            log.info("Audio sintetizado exitosamente ({} bytes, {} seg)", audioBytes.length, durationSeconds);

            return NarrationResult.mp3(audioBytes, durationSeconds, voiceUsed);

        } catch (IOException e) {
            throw new NarrationException("Error de I/O ejecutando Edge-TTS: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NarrationException("Proceso de síntesis de voz interrumpido", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            try {
                Files.deleteIfExists(tempOutputFile);
            } catch (IOException ignored) {}
        }
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private String resolvePythonExecutable(String configured) {
        String[] candidates = {configured, "python3", "python", "py"};
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) continue;
            try {
                Process p = new ProcessBuilder(candidate, "--version").start();
                if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        return "python";
    }

    private Path resolveScriptPath() {
        // 1. Buscar en ./scripts/story_tts_bridge.py
        Path localPath = Paths.get("scripts", "story_tts_bridge.py").toAbsolutePath().normalize();
        if (Files.exists(localPath)) {
            return localPath;
        }

        // 2. Buscar en /app/scripts/story_tts_bridge.py (Contenedor)
        Path containerPath = Paths.get("/app/scripts/story_tts_bridge.py");
        if (Files.exists(containerPath)) {
            return containerPath;
        }

        // 3. Fallback: buscar en classpath y extraer a temp
        try {
            InputStream is = getClass().getResourceAsStream("/scripts/story_tts_bridge.py");
            if (is != null) {
                Path extracted = Paths.get("temp", "story_tts_bridge.py").toAbsolutePath().normalize();
                Files.createDirectories(extracted.getParent());
                Files.copy(is, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return extracted;
            }
        } catch (Exception ignored) {}

        return localPath;
    }
}
