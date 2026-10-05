package com.kidsanim.api.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.infrastructure.config.KidsLlmProperties;
import com.kidsanim.api.infrastructure.exception.LlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("geminiLlmProvider")
public class GeminiLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmProvider.class);
    private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public GeminiLlmProvider(KidsLlmProperties llmProperties, ObjectMapper objectMapper) {
        this.apiKey = llmProperties.gemini().apiKey();
        this.model = llmProperties.gemini().model();
        this.timeoutSeconds = llmProperties.gemini().timeoutSeconds();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .sslContext(createPermissiveSslContext())
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public GeminiLlmProvider(String apiKey, String model, int timeoutSeconds, ObjectMapper objectMapper, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String generateStructured(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        if (!isAvailable()) {
            throw new LlmException("Gemini API Key no configurada o vacía", "gemini");
        }

        log.info("Llamando a Google Gemini ({}) para generación estructurada...", model);

        try {
            Map<String, Object> requestBody = new HashMap<>();

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                requestBody.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ));
            }

            requestBody.put("contents", List.of(
                    Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))
            ));

            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("response_mime_type", "application/json");
            if (jsonSchema != null && !jsonSchema.isEmpty()) {
                genConfig.put("response_schema", jsonSchema);
            }
            requestBody.put("generationConfig", genConfig);

            String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);
            String payload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini respondió con error {}: {}", response.statusCode(), response.body());
                throw new LlmException("Gemini HTTP " + response.statusCode() + ": " + response.body(), "gemini");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidate = root.path("candidates").path(0);
            JsonNode parts = candidate.path("content").path("parts");

            if (parts.isArray() && !parts.isEmpty()) {
                String text = parts.get(0).path("text").asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }

            throw new LlmException("Gemini devolvió respuesta sin texto utilizable: " + response.body(), "gemini");

        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al comunicarse con Gemini: {}", e.getMessage(), e);
            throw new LlmException("Error al comunicarse con Gemini: " + e.getMessage(), "gemini", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "gemini";
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
            try {
                return SSLContext.getDefault();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
