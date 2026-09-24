package com.trivia.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trivia.api.ai.dto.AiGenerationRequest;
import com.trivia.api.ai.dto.AiTriviaItem;
import com.trivia.api.ai.exception.AiClientException;
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
import java.util.List;

/**
 * Implementación de TriviaAiClient para Google Gemini (Gemini 2.0 Flash).
 *
 * Utiliza structured outputs (JSON Schema) nativo de la API de Gemini
 * para garantizar respuestas predecibles y estrictamente tipadas.
 */
@Component
public class GeminiTriviaAiClient implements TriviaAiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiTriviaAiClient.class);
    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public GeminiTriviaAiClient(
            @Value("${trivia.ai.api-key:}") String apiKey,
            @Value("${trivia.ai.model:gemini-3.5-flash-lite}") String model,
            @Value("${trivia.ai.timeout-seconds:30}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this(apiKey, model, timeoutSeconds, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build());
    }

    // Constructor con inyección de HttpClient para tests unitarios
    public GeminiTriviaAiClient(
            String apiKey,
            String model,
            int timeoutSeconds,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public List<AiTriviaItem> generate(AiGenerationRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiClientException("La clave de API de IA (AI_API_KEY) no está configurada.");
        }

        String prompt = buildPrompt(request);
        String requestBodyJson = buildRequestBodyJson(prompt);

        String endpointUrl = String.format(GEMINI_API_URL_TEMPLATE, model, apiKey);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        try {
            log.info("Enviando solicitud a Gemini ({}) para generar {} trivias de tipo {}",
                    model, request.cantidad(), request.tipoTrivia());

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Error devuelto por Gemini API [HTTP {}]: {}", response.statusCode(), response.body());
                throw new AiClientException("Error devuelto por Gemini API: " + response.body(), response.statusCode());
            }

            return parseGeminiResponse(response.body());
        } catch (IOException e) {
            log.error("Fallo de E/S al comunicarse con Gemini API", e);
            throw new AiClientException("Error de red al comunicar con Gemini: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Llamada a Gemini interrumpida: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(AiGenerationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un generador experto de contenido educativo y preguntas de trivia de alta calidad.\n\n");
        sb.append("Debes generar exactamente ").append(request.cantidad()).append(" preguntas de trivia.\n");
        sb.append("- Categoría/Tipo: ").append(request.tipoTrivia()).append("\n");

        if (request.subtema() != null && !request.subtema().isBlank()) {
            sb.append("- Subtema específico: ").append(request.subtema()).append("\n");
        }

        sb.append("- Dificultad: ").append(request.dificultad().name()).append("\n");
        sb.append("- Idioma: ").append(request.idioma()).append("\n");
        sb.append("- Cada pregunta debe tener exactamente ").append(request.numeroOpciones()).append(" opciones de respuesta.\n");
        sb.append("- Las letras de las opciones deben ser secuenciales: ");
        for (int i = 0; i < request.numeroOpciones(); i++) {
            sb.append((char) ('A' + i)).append(i < request.numeroOpciones() - 1 ? ", " : ".\n");
        }
        sb.append("- Exactamente UNA opción debe tener 'correcta' = true, y las demás 'correcta' = false.\n");
        sb.append("- La explicación debe ser informativa, verídica y redactada en tono educativo.\n");

        if (!request.preguntasExistentes().isEmpty()) {
            sb.append("\nIMPORTANTE — MEMORIA DEL SISTEMA:\n");
            sb.append("Las siguientes preguntas ya existen en nuestra base de datos. Está estrictamente PROHIBIDO ");
            sb.append("generar preguntas repetidas o conceptualmente equivalentes a cualquiera de las siguientes:\n");
            for (String exist : request.preguntasExistentes()) {
                sb.append(" * ").append(exist).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildRequestBodyJson(String promptText) {
        ObjectNode root = objectMapper.createObjectNode();

        // contents
        ArrayNode contents = root.putArray("contents");
        ObjectNode contentItem = contents.addObject();
        contentItem.put("role", "user");
        ArrayNode parts = contentItem.putArray("parts");
        parts.addObject().put("text", promptText);

        // generationConfig with structured schema
        ObjectNode genConfig = root.putObject("generationConfig");
        genConfig.put("responseMimeType", "application/json");

        ObjectNode schema = genConfig.putObject("responseSchema");
        schema.put("type", "ARRAY");

        ObjectNode itemSchema = schema.putObject("items");
        itemSchema.put("type", "OBJECT");

        ObjectNode props = itemSchema.putObject("properties");
        props.putObject("pregunta").put("type", "STRING");

        ObjectNode opcionesProp = props.putObject("opciones");
        opcionesProp.put("type", "ARRAY");
        ObjectNode opcionItem = opcionesProp.putObject("items");
        opcionItem.put("type", "OBJECT");
        ObjectNode opcionProps = opcionItem.putObject("properties");
        opcionProps.putObject("letra").put("type", "STRING");
        opcionProps.putObject("texto").put("type", "STRING");
        opcionProps.putObject("correcta").put("type", "BOOLEAN");
        ArrayNode opcionReq = opcionItem.putArray("required");
        opcionReq.add("letra").add("texto").add("correcta");

        props.putObject("explicacion").put("type", "STRING");

        ArrayNode itemReq = itemSchema.putArray("required");
        itemReq.add("pregunta").add("opciones").add("explicacion");

        return root.toString();
    }

    private List<AiTriviaItem> parseGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty()) {
                throw new AiClientException("Gemini no retornó candidatos de respuesta.");
            }

            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new AiClientException("El contenido generado por Gemini está vacío.");
            }

            String rawText = textNode.asText().trim();

            // Limpieza de posibles delimitadores Markdown (```json ... ```)
            if (rawText.startsWith("```json")) {
                rawText = rawText.substring(7);
            } else if (rawText.startsWith("```")) {
                rawText = rawText.substring(3);
            }
            if (rawText.endsWith("```")) {
                rawText = rawText.substring(0, rawText.length() - 3);
            }
            rawText = rawText.trim();

            return objectMapper.readValue(rawText, new TypeReference<List<AiTriviaItem>>() {});
        } catch (AiClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al deserializar la respuesta estructurada de Gemini: {}", responseJson, e);
            throw new AiClientException("Error deserializando la respuesta estructurada de Gemini: " + e.getMessage(), e);
        }
    }
}
