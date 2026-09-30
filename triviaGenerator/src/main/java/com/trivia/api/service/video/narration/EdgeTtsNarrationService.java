package com.trivia.api.service.video.narration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.service.video.TimelinePlan;
import com.trivia.api.service.video.TriviaSceneTiming;
import com.trivia.api.service.video.VideoFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Implementación de NarrationService que utiliza Edge-TTS (voces neuronales Microsoft)
 * a través del puente Python 'scripts/tts_bridge.py'.
 *
 * Características:
 *   - Si withTts = false: Asigna duraciones calibradas fijas (7.0s + 5.0s + 6.5s) sin generar audio.
 *   - Si withTts = true: Sintetiza en tiempo real los audios MP3 para preguntas y respuestas,
 *     mide sus duraciones reales y ajusta de forma adaptativa cada escena del video.
 *   - Fallback resiliente: Si la síntesis TTS falla (ej. sin conexión a internet),
 *     conmuta de forma automática y transparente al modo sin voz, permitiendo que el video se genere.
 */
@Service("ttsNarrationService")
@Primary
public class EdgeTtsNarrationService implements NarrationService {

    private static final Logger log = LoggerFactory.getLogger(EdgeTtsNarrationService.class);

    private final ObjectMapper objectMapper;

    private final double defaultQuestionDuration;
    private final double defaultCountdownDuration;
    private final double defaultAnswerDuration;
    private final String pythonExecutable;

    public record TtsBridgeResult(
            String triviaId,
            int index,
            String questionAudioPath,
            double questionDuration,
            String answerAudioPath,
            double answerDuration
    ) {}

    public EdgeTtsNarrationService(
            ObjectMapper objectMapper,
            @Value("${trivia.video.timing.question:7.0}") double defaultQuestionDuration,
            @Value("${trivia.video.timing.countdown:5.0}") double defaultCountdownDuration,
            @Value("${trivia.video.timing.answer:6.5}") double defaultAnswerDuration,
            @Value("${trivia.video.python-path:python}") String pythonExecutable) {
        this.objectMapper = objectMapper;
        this.defaultQuestionDuration = defaultQuestionDuration;
        this.defaultCountdownDuration = defaultCountdownDuration;
        this.defaultAnswerDuration = defaultAnswerDuration;
        this.pythonExecutable = pythonExecutable;
    }

    @Override
    public TimelinePlan planTimeline(
            List<Trivia> trivias,
            Map<Trivia, List<TriviaOpcion>> opcionesMap,
            VideoFormat format,
            boolean withBgm,
            boolean withTts,
            String voice) {

        if (!withTts) {
            log.debug("TTS desactivado. Generando plan con duraciones estándar para {} trivias", trivias.size());
            return planFixedTimeline(trivias, format, withBgm);
        }

        log.info("Iniciando síntesis TTS para {} trivias usando voz '{}'", trivias.size(), voice);

        try {
            Map<UUID, TtsBridgeResult> ttsResults = synthesizeViaBridge(trivias, opcionesMap, voice);
            List<TriviaSceneTiming> scenes = new ArrayList<>();
            int total = trivias.size();

            for (int i = 0; i < total; i++) {
                Trivia trivia = trivias.get(i);
                int index = i + 1;
                String headerText = String.format("PREGUNTA %d DE %d", index, total);

                TtsBridgeResult res = ttsResults.get(trivia.getId());
                if (res != null) {
                    double qDur = Math.max(6.0, res.questionDuration() + 1.2);
                    double cDur = defaultCountdownDuration;
                    double aDur = Math.max(6.0, res.answerDuration() + 1.5);

                    scenes.add(new TriviaSceneTiming(
                            trivia.getId(),
                            index,
                            total,
                            headerText,
                            qDur,
                            cDur,
                            aDur,
                            res.questionAudioPath(),
                            res.answerAudioPath()
                    ));
                } else {
                    scenes.add(new TriviaSceneTiming(
                            trivia.getId(),
                            index,
                            total,
                            headerText,
                            defaultQuestionDuration,
                            defaultCountdownDuration,
                            defaultAnswerDuration,
                            null,
                            null
                    ));
                }
            }

            return new TimelinePlan(scenes, format, withBgm);

        } catch (Exception e) {
            log.warn("Fallo en síntesis TTS ({}). Conmutando a modo de respaldo sin locución.", e.getMessage());
            return planFixedTimeline(trivias, format, withBgm);
        }
    }

    private TimelinePlan planFixedTimeline(List<Trivia> trivias, VideoFormat format, boolean withBgm) {
        List<TriviaSceneTiming> scenes = new ArrayList<>();
        int total = trivias.size();

        for (int i = 0; i < total; i++) {
            Trivia trivia = trivias.get(i);
            int index = i + 1;
            String headerText = String.format("PREGUNTA %d DE %d", index, total);

            scenes.add(new TriviaSceneTiming(
                    trivia.getId(),
                    index,
                    total,
                    headerText,
                    defaultQuestionDuration,
                    defaultCountdownDuration,
                    defaultAnswerDuration,
                    null,
                    null
            ));
        }

        return new TimelinePlan(scenes, format, withBgm);
    }

    private Map<UUID, TtsBridgeResult> synthesizeViaBridge(
            List<Trivia> trivias,
            Map<Trivia, List<TriviaOpcion>> opcionesMap,
            String voice) throws Exception {

        Path tempDir = Files.createTempDirectory("trivia_tts_job_");
        Path inputJsonPath = tempDir.resolve("trivias_payload.json");
        Path audioOutputDir = tempDir.resolve("audio");
        Files.createDirectories(audioOutputDir);

        // Estructurar DTO para tts_bridge.py
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Trivia t : trivias) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId().toString());
            map.put("pregunta", t.getPregunta());
            map.put("explicacion", t.getExplicacion());

            List<TriviaOpcion> opciones = opcionesMap != null && opcionesMap.containsKey(t)
                    ? opcionesMap.get(t)
                    : List.of();

            List<Map<String, Object>> opcList = new ArrayList<>();
            for (TriviaOpcion op : opciones) {
                opcList.add(Map.of(
                        "letra", op.getLetra(),
                        "texto", op.getTexto(),
                        "correcta", Boolean.TRUE.equals(op.getCorrecta())
                ));
            }
            map.put("opciones", opcList);
            payload.add(map);
        }

        objectMapper.writeValue(inputJsonPath.toFile(), payload);

        Path scriptPath = resolveScriptPath();
        String pythonBin = resolvePythonExecutable();

        List<String> cmd = List.of(
                pythonBin,
                scriptPath.toString(),
                "--input-json", inputJsonPath.toString(),
                "--output-dir", audioOutputDir.toString(),
                "--voice", (voice != null && !voice.isBlank()) ? voice : "es-MX-JorgeNeural",
                "--rate", "+5%"
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        try (Scanner sc = new Scanner(process.getInputStream())) {
            while (sc.hasNextLine()) {
                stdout.append(sc.nextLine()).append("\n");
            }
        }

        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("El proceso de TTS excedió el límite de 120s");
        }

        if (process.exitValue() != 0) {
            String err;
            try (InputStream is = process.getErrorStream()) {
                err = new String(is.readAllBytes());
            }
            throw new IllegalStateException("Error en tts_bridge.py (exit code " + process.exitValue() + "): " + err);
        }

        List<TtsBridgeResult> results = objectMapper.readValue(
                stdout.toString().trim(),
                new TypeReference<>() {}
        );

        Map<UUID, TtsBridgeResult> map = new HashMap<>();
        for (TtsBridgeResult r : results) {
            map.put(UUID.fromString(r.triviaId()), r);
        }
        return map;
    }

    private Path resolveScriptPath() throws Exception {
        // 1. Verificar ruta relativa scripts/tts_bridge.py
        Path path = Paths.get("scripts", "tts_bridge.py").toAbsolutePath().normalize();
        if (Files.exists(path)) {
            return path;
        }
        // 2. Verificar en directorio /app/scripts/tts_bridge.py
        Path appPath = Paths.get("/app", "scripts", "tts_bridge.py");
        if (Files.exists(appPath)) {
            return appPath;
        }
        // 3. Fallback: extraer desde el classpath resource
        try (InputStream is = getClass().getResourceAsStream("/scripts/tts_bridge.py")) {
            if (is != null) {
                Path tempScript = Files.createTempFile("tts_bridge_", ".py");
                Files.copy(is, tempScript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                tempScript.toFile().deleteOnExit();
                return tempScript;
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer tts_bridge.py desde classpath: {}", e.getMessage());
        }
        throw new IllegalStateException("Script no encontrado en filesystem ni en resources: " + path);
    }

    private String resolvePythonExecutable() {
        if (pythonExecutable != null && !pythonExecutable.isBlank() && !"python".equalsIgnoreCase(pythonExecutable)) {
            return pythonExecutable;
        }
        for (String candidate : List.of("python3", "python", "py")) {
            try {
                Process p = new ProcessBuilder(candidate, "--version").start();
                if (p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        return "python";
    }
}
