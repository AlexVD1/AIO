package com.kidsanim.api.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.infrastructure.config.ExternalServicesProperties;
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

@Component("ollamaLlmProvider")
public class OllamaLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmProvider.class);

    private final String ollamaUrl;
    private final String model;
    private final double temperature;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OllamaLlmProvider(ExternalServicesProperties servicesProperties,
                             KidsLlmProperties llmProperties,
                             ObjectMapper objectMapper) {
        this.ollamaUrl = servicesProperties.ollamaUrl();
        this.model = llmProperties.ollama().model();
        this.temperature = llmProperties.ollama().temperature();
        this.timeoutSeconds = llmProperties.ollama().timeoutSeconds();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .sslContext(createPermissiveSslContext())
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public OllamaLlmProvider(String ollamaUrl,
                             String model,
                             double temperature,
                             int timeoutSeconds,
                             ObjectMapper objectMapper,
                             HttpClient httpClient) {
        this.ollamaUrl = ollamaUrl;
        this.model = model;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String generateStructured(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        log.info("Llamando a Ollama ({}) en {} para generación estructurada...", model, ollamaUrl);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            if (jsonSchema != null && !jsonSchema.isEmpty()) {
                requestBody.put("format", jsonSchema);
            }
            requestBody.put("stream", false);
            requestBody.put("options", Map.of("temperature", temperature));

            String payload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl.replaceAll("/+$", "") + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama respondió con error {}: {}", response.statusCode(), response.body());
                throw new LlmException("Ollama HTTP " + response.statusCode() + ": " + response.body(), "ollama");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode messageNode = root.path("message");
            String content = messageNode.path("content").asText(null);

            if (content == null || content.isBlank()) {
                throw new LlmException("Ollama devolvió contenido vacío en la respuesta", "ollama");
            }

            log.debug("Respuesta recibida de Ollama ({} chars)", content.length());
            return content.trim();

        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fallo al comunicarse con Ollama: {}", e.getMessage(), e);
            throw new LlmException("Error al comunicarse con Ollama: " + e.getMessage(), "ollama", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl.replaceAll("/+$", "") + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<Void> resp = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
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
