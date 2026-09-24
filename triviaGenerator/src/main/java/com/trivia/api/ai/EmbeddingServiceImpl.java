package com.trivia.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trivia.api.ai.exception.AiClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Implementación de EmbeddingService con soporte para Google Gemini (text-embedding-004)
 * y cálculo matemático de similitud coseno de alta precisión.
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
    private static final String EMBEDDING_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=%s";

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public EmbeddingServiceImpl(
            @Value("${trivia.ai.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this(apiKey, objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public EmbeddingServiceImpl(String apiKey, ObjectMapper objectMapper, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.debug("AI_API_KEY no configurada. Generando embedding pseudo-vectorial para fallback local.");
            return generateLocalPseudoEmbedding(text);
        }

        try {
            String endpointUrl = String.format(EMBEDDING_URL_TEMPLATE, apiKey);

            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", "models/text-embedding-004");
            ObjectNode content = root.putObject("content");
            content.putArray("parts").addObject().put("text", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Fallo al obtener embedding de Gemini [HTTP {}]. Usando fallback local.", response.statusCode());
                return generateLocalPseudoEmbedding(text);
            }

            JsonNode responseRoot = objectMapper.readTree(response.body());
            JsonNode valuesNode = responseRoot.path("embedding").path("values");

            if (!valuesNode.isArray() || valuesNode.isEmpty()) {
                return generateLocalPseudoEmbedding(text);
            }

            float[] vector = new float[valuesNode.size()];
            for (int i = 0; i < valuesNode.size(); i++) {
                vector[i] = (float) valuesNode.get(i).asDouble();
            }
            return vector;
        } catch (IOException | InterruptedException e) {
            log.warn("Error de red al consultar Gemini text-embedding-004. Usando fallback.", e);
            return generateLocalPseudoEmbedding(text);
        }
    }

    @Override
    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length == 0 || vectorB.length == 0) {
            return 0.0;
        }
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException(String.format(
                    "Dimensiones de vectores incompatibles: %d vs %d", vectorA.length, vectorB.length));
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA <= 0.0 || normB <= 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public boolean isSemanticallySimilar(float[] vectorA, float[] vectorB, double threshold) {
        return cosineSimilarity(vectorA, vectorB) >= threshold;
    }

    @Override
    public String serialize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new IllegalStateException("Error al serializar vector de embedding", e);
        }
    }

    @Override
    public float[] deserialize(String serialized) {
        if (serialized == null || serialized.isBlank() || serialized.equals("[]")) {
            return new float[0];
        }
        try {
            List<Double> list = objectMapper.readValue(serialized, new TypeReference<List<Double>>() {});
            float[] vector = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                vector[i] = list.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            log.error("Error al deserializar embedding desde texto: {}", serialized, e);
            return new float[0];
        }
    }

    /**
     * Fallback determinista: genera un vector normalizado de 64 dimensiones basado
     * en n-gramas de caracteres para entornos offline o sin clave de API.
     */
    private float[] generateLocalPseudoEmbedding(String text) {
        int dim = 64;
        float[] vector = new float[dim];
        String clean = text.toLowerCase().trim();

        for (int i = 0; i < clean.length(); i++) {
            int charCode = clean.charAt(i);
            int idx = Math.abs((charCode * 31 + i)) % dim;
            vector[idx] += 1.0f;
        }

        // Normalizar a vector unitario (norma L2 = 1.0)
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm > 0.0) {
            float sqrtNorm = (float) Math.sqrt(norm);
            for (int i = 0; i < dim; i++) {
                vector[i] /= sqrtNorm;
            }
        }
        return vector;
    }
}
