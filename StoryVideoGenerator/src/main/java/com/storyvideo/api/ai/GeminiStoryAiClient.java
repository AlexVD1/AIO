package com.storyvideo.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.storyvideo.api.ai.dto.AiCharacterItem;
import com.storyvideo.api.ai.dto.AiSceneItem;
import com.storyvideo.api.ai.dto.AiStoryGenerationRequest;
import com.storyvideo.api.ai.dto.AiStoryResponse;
import com.storyvideo.api.ai.exception.AiClientException;
import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.NarrativeArc;
import com.storyvideo.api.domain.SceneType;
import com.storyvideo.api.domain.TransitionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Component
public class GeminiStoryAiClient implements StoryAiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiStoryAiClient.class);
    private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public GeminiStoryAiClient(
            @Value("${story.ai.api-key:}") String apiKey,
            @Value("${story.ai.model:gemini-3.5-flash-lite}") String model,
            @Value("${story.ai.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this(apiKey, model, timeoutSeconds, objectMapper,
                HttpClient.newBuilder()
                        .sslContext(createPermissiveSslContext())
                        .connectTimeout(Duration.ofSeconds(15))
                        .build());
    }

    private static SSLContext createPermissiveSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            return sc;
        } catch (Exception e) {
            log.warn("No se pudo inicializar SSLContext permisivo: {}", e.getMessage());
            try {
                return SSLContext.getDefault();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public GeminiStoryAiClient(
            String apiKey,
            String model,
            int timeoutSeconds,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model : "gemini-2.5-flash";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 120;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank();
    }

    @Override
    public AiStoryResponse generateStory(AiStoryGenerationRequest request) {
        if (!isAvailable()) {
            throw new AiClientException("La clave de API de Gemini (AI_API_KEY) no está configurada.");
        }

        String prompt = buildPrompt(request);
        String requestBody = buildRequestBody(prompt);
        String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Enviando solicitud de generación de historia a Gemini ({}, intento {}/{}) para género: {}",
                        model, attempt, maxAttempts, request.genre());
                response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                }

                if ((response.statusCode() == 503 || response.statusCode() == 429) && attempt < maxAttempts) {
                    long sleepMs = (response.statusCode() == 429) ? (attempt * 15000L) : 3000L;
                    log.warn("Gemini API respondió {} en intento {}. Esperando {}s para respetar rate limit...",
                            response.statusCode(), attempt, sleepMs / 1000);
                    Thread.sleep(sleepMs);
                    continue;
                }

                log.error("Fallo en Gemini API: HTTP {} - {}", response.statusCode(), response.body());
                throw new AiClientException("Error devuelto por Gemini API: HTTP " + response.statusCode(), response.statusCode());

            } catch (IOException e) {
                if (attempt < maxAttempts) {
                    log.warn("Error I/O con Gemini en intento {}. Reintentando en 3s...", attempt);
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    continue;
                }
                throw new AiClientException("Error de I/O al comunicarse con Gemini API: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiClientException("Llamada a Gemini API interrumpida", e);
            }
        }
        throw new AiClientException("No se pudo obtener respuesta de Gemini tras " + maxAttempts + " intentos");
    }

    private String buildPrompt(AiStoryGenerationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un director narrativo y guionista experto en contenido audiovisual corto de alta retención (estilo TikTok, YouTube Shorts, Reels y Universo NullØ).\n");
        sb.append("Crea una historia original, inmersiva, atmosférica y con ritmo cinematográfico.\n\n");

        sb.append("DIRECTRICES DE CONTENIDO:\n");
        sb.append("- Género: ").append(request.genre()).append("\n");
        sb.append("- Tono: ").append(request.tone()).append("\n");
        if (request.theme() != null && !request.theme().isBlank()) {
            sb.append("- Tema central / disparador: ").append(request.theme()).append("\n");
        }
        sb.append("- Idioma de la narración: ").append(request.language()).append("\n");
        sb.append("- Duración estimada total: ").append(request.targetDurationSeconds()).append(" segundos.\n");
        sb.append("- Número exacto de escenas requeridas: ").append(request.sceneCount() > 0 ? request.sceneCount() : 5).append(" escenas.\n");
        sb.append("- Longitud de locución por escena: Cada escena DEBE contener una narración ('narrationText') sustanciosa y desarrollada de aproximadamente 25 a 35 palabras, de modo que la duración total hablada del video se sitúe consistentemente entre 65 y 85 segundos.\n\n");

        sb.append("ESTRUCTURA OBLIGATORIA DEL HOOK (Primeros 3 segundos):\n");
        sb.append("- La primera frase ('hook') debe generar intriga instantánea, una pregunta perturbadora o un hecho insólito.\n");
        sb.append("- La Escena 1 debe tener sceneType 'HOOK', con una locución que atrape de inmediato.\n\n");

        sb.append("DIRECTRICES PARA GENERACIÓN VISUAL (Stable Diffusion SDXL):\n");
        if (request.visualStyleGuidance() != null && !request.visualStyleGuidance().isBlank()) {
            sb.append("- Estilo artístico asignado: ").append(request.visualStyleGuidance()).append("\n");
            sb.append("- En cada escena, 'visualPrompt' DEBE estar escrito en inglés y describir la escena adaptada estrictamente a este estilo artístico (ej. si es 2D illustration, comic book, cartoon, low-poly, cel shading, etc.). PROHIBIDO usar 'photorealistic', '35mm photograph' o lenguaje hiperrealista a menos que el estilo lo pida.\n");
        } else {
            sb.append("- En cada escena, 'visualPrompt' DEBE estar escrito en inglés, describiendo la toma visualmente con lenguaje evocador y artístico.\n");
        }
        sb.append("- No uses texto ni marcas de agua en visualPrompt.\n\n");

        if (request.recentPremisesToAvoid() != null && !request.recentPremisesToAvoid().isEmpty()) {
            sb.append("MEMORIA HISTÓRICA — PROHIBIDO REPETIR O PARAFRASEAR LAS SIGUIENTES PREMISAS RECIENTES:\n");
            for (String premise : request.recentPremisesToAvoid()) {
                sb.append(" * ").append(premise).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Genera un JSON estructurado con título, hook, premisa, sinopsis, arco narrativo, twist (giro final inesperado), desenlace, lista de personajes principales y desglose detallado de cada escena.");
        return sb.toString();
    }

    private String buildRequestBody(String prompt) {
        ObjectNode root = objectMapper.createObjectNode();

        // Contents
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        // Generation Config con JSON Schema nativo
        ObjectNode genConfig = root.putObject("generationConfig");
        genConfig.put("responseMimeType", "application/json");
        genConfig.set("responseSchema", buildJsonSchema());

        return root.toString();
    }

    private ObjectNode buildJsonSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");
        ObjectNode props = schema.putObject("properties");

        props.putObject("title").put("type", "STRING");
        props.putObject("hook").put("type", "STRING");
        props.putObject("premise").put("type", "STRING");
        props.putObject("synopsis").put("type", "STRING");
        props.putObject("narrativeArc").put("type", "STRING");
        props.putObject("twist").put("type", "STRING");
        props.putObject("ending").put("type", "STRING");

        // Characters array
        ObjectNode charsNode = props.putObject("characters");
        charsNode.put("type", "ARRAY");
        ObjectNode charItem = charsNode.putObject("items");
        charItem.put("type", "OBJECT");
        ObjectNode charProps = charItem.putObject("properties");
        charProps.putObject("name").put("type", "STRING");
        charProps.putObject("physicalDescription").put("type", "STRING");
        charProps.putObject("distinctiveFeatures").put("type", "STRING");
        charProps.putObject("role").put("type", "STRING");
        charProps.putObject("promptFragment").put("type", "STRING");

        // Scenes array
        ObjectNode scenesNode = props.putObject("scenes");
        scenesNode.put("type", "ARRAY");
        ObjectNode sceneItem = scenesNode.putObject("items");
        sceneItem.put("type", "OBJECT");
        ObjectNode sceneProps = sceneItem.putObject("properties");
        sceneProps.putObject("sequenceNumber").put("type", "INTEGER");
        sceneProps.putObject("sceneType").put("type", "STRING");
        sceneProps.putObject("narrationText").put("type", "STRING");
        sceneProps.putObject("narrationEmotion").put("type", "STRING");
        sceneProps.putObject("visualDescription").put("type", "STRING");
        sceneProps.putObject("visualPrompt").put("type", "STRING");
        sceneProps.putObject("cameraMovement").put("type", "STRING");
        sceneProps.putObject("musicIntensity").put("type", "STRING");
        sceneProps.putObject("ambientSound").put("type", "STRING");
        sceneProps.putObject("transitionIn").put("type", "STRING");
        sceneProps.putObject("transitionOut").put("type", "STRING");

        ArrayNode sceneRequired = sceneItem.putArray("required");
        sceneRequired.add("sequenceNumber");
        sceneRequired.add("sceneType");
        sceneRequired.add("narrationText");
        sceneRequired.add("visualPrompt");
        sceneRequired.add("visualDescription");

        ArrayNode required = schema.putArray("required");
        required.add("title");
        required.add("hook");
        required.add("premise");
        required.add("synopsis");
        required.add("ending");
        required.add("scenes");

        return schema;
    }

    private AiStoryResponse parseResponse(String responseBodyJson) {
        try {
            JsonNode root = objectMapper.readTree(responseBodyJson);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new AiClientException("Respuesta vacía o sin candidatos en Gemini API");
            }

            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new AiClientException("Candidato de Gemini sin contenido textual");
            }

            JsonNode data = objectMapper.readTree(textNode.asText());

            String title = data.path("title").asText("");
            String hook = data.path("hook").asText("");
            String premise = data.path("premise").asText("");
            String synopsis = data.path("synopsis").asText("");
            String twist = data.path("twist").asText("");
            String ending = data.path("ending").asText("");

            NarrativeArc arc = parseEnum(data.path("narrativeArc").asText("LINEAR"), NarrativeArc.class, NarrativeArc.LINEAR);

            List<AiCharacterItem> characters = new ArrayList<>();
            JsonNode charsNode = data.path("characters");
            if (charsNode.isArray()) {
                for (JsonNode c : charsNode) {
                    characters.add(new AiCharacterItem(
                            c.path("name").asText(""),
                            c.path("physicalDescription").asText(""),
                            c.path("distinctiveFeatures").asText(""),
                            c.path("role").asText("PROTAGONIST"),
                            c.path("promptFragment").asText("")
                    ));
                }
            }

            List<AiSceneItem> scenes = new ArrayList<>();
            JsonNode scenesNode = data.path("scenes");
            if (scenesNode.isArray()) {
                int seq = 1;
                for (JsonNode s : scenesNode) {
                    int sequenceNumber = s.path("sequenceNumber").asInt(seq);
                    SceneType sceneType = parseEnum(s.path("sceneType").asText("DEVELOPMENT"), SceneType.class, SceneType.DEVELOPMENT);
                    String narrationText = s.path("narrationText").asText("");
                    if (narrationText.isBlank()) narrationText = s.path("narration").asText("");
                    if (narrationText.isBlank()) narrationText = s.path("voiceover").asText("");
                    if (narrationText.isBlank()) narrationText = s.path("script").asText("");
                    if (narrationText.isBlank()) narrationText = s.path("text").asText("");
                    if (narrationText.isBlank()) narrationText = s.path("dialogue").asText("");
                    if (narrationText.isBlank()) narrationText = s.path("speech").asText("");
                    if (narrationText.isBlank()) narrationText = s.path("visualDescription").asText("");

                    String visualPrompt = s.path("visualPrompt").asText("");
                    if (visualPrompt.isBlank()) visualPrompt = s.path("prompt").asText("");
                    if (visualPrompt.isBlank()) visualPrompt = s.path("imagePrompt").asText("");
                    if (visualPrompt.isBlank()) visualPrompt = s.path("visualDescription").asText("");

                    String visualDescription = s.path("visualDescription").asText("");
                    if (visualDescription.isBlank()) visualDescription = visualPrompt;

                    if (narrationText.isBlank()) {
                        narrationText = "En este punto clave del relato, los acontecimientos toman una direccion inesperada que desafia lo evidente.";
                    }
                    if (visualPrompt.isBlank() || visualPrompt.trim().length() < 10) {
                        visualPrompt = "Cinematic shot with dramatic lighting, artistic details, highly evocative atmosphere, 8k resolution";
                    }

                    String narrationEmotion = s.path("narrationEmotion").asText("NEUTRAL");
                    CameraMovement cam = parseEnum(s.path("cameraMovement").asText("KEN_BURNS"), CameraMovement.class, CameraMovement.KEN_BURNS);
                    String musicIntensity = s.path("musicIntensity").asText("MEDIUM");
                    String ambientSound = s.path("ambientSound").asText(null);
                    TransitionType transIn = parseEnum(s.path("transitionIn").asText("FADE_IN"), TransitionType.class, TransitionType.FADE_IN);
                    TransitionType transOut = parseEnum(s.path("transitionOut").asText("CUT"), TransitionType.class, TransitionType.CUT);

                    scenes.add(new AiSceneItem(
                            sequenceNumber, sceneType, narrationText, narrationEmotion,
                            visualDescription, visualPrompt, cam, musicIntensity,
                            ambientSound, transIn, transOut
                    ));
                    seq++;
                }
            }

            return new AiStoryResponse(title, hook, premise, synopsis, arc, twist, ending, characters, scenes);

        } catch (Exception e) {
            log.error("Error deserializando respuesta de Gemini: {}", e.getMessage(), e);
            throw new AiClientException("Error parseando salida estructurada de Gemini: " + e.getMessage(), e);
        }
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass, E defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
