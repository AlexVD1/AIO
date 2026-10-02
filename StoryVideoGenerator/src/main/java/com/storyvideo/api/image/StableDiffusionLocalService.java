package com.storyvideo.api.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.storyvideo.api.image.dto.GeneratedImage;
import com.storyvideo.api.image.dto.ImageGenerationRequest;
import com.storyvideo.api.image.exception.ImageGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Service("stableDiffusionLocalService")
@Primary
public class StableDiffusionLocalService implements ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(StableDiffusionLocalService.class);

    private final String sdApiUrl;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public StableDiffusionLocalService(
            @Value("${story.image.sd-api-url:http://localhost:7860}") String sdApiUrl,
            @Value("${story.image.timeout-seconds:120}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this(sdApiUrl, timeoutSeconds, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build());
    }

    public StableDiffusionLocalService(
            String sdApiUrl,
            int timeoutSeconds,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        String url = sdApiUrl != null ? sdApiUrl.trim() : "http://localhost:7860";
        this.sdApiUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 120;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String getProviderName() {
        return "Stable Diffusion WebUI Forge (Local GPU RTX 5070)";
    }

    @Override
    public int getEstimatedTimeSeconds() {
        return 12; // ~8-12 segundos por imagen en RTX 5070 con SDXL
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sdApiUrl + "/sdapi/v1/progress"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Forge API no responde en {}: {}", sdApiUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public GeneratedImage generate(ImageGenerationRequest request) {
        long startTime = System.currentTimeMillis();
        String endpoint = sdApiUrl + "/sdapi/v1/txt2img";

        log.info("Invocando SD Forge en {} para escena {} (seed: {})", endpoint, request.sequenceNumber(), request.resolvedSeed());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", request.prompt());
        payload.put("negative_prompt", request.negativePrompt() != null ? request.negativePrompt() : "");
        payload.put("steps", request.resolvedSteps());
        payload.put("cfg_scale", request.resolvedCfgScale());
        payload.put("width", request.resolvedWidth());
        payload.put("height", request.resolvedHeight());
        payload.put("sampler_name", request.resolvedSampler());
        payload.put("seed", request.resolvedSeed());
        payload.put("batch_size", 1);
        payload.put("n_iter", 1);

        String jsonBody = payload.toString();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Fallo en Forge API txt2img: HTTP {} - {}", response.statusCode(), response.body());
                throw new ImageGenerationException("Error devuelto por Stable Diffusion Forge: HTTP " + response.statusCode(), response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode imagesNode = root.path("images");

            if (!imagesNode.isArray() || imagesNode.isEmpty()) {
                throw new ImageGenerationException("Forge API respondió sin imágenes en el array 'images'");
            }

            String base64Image = imagesNode.get(0).asText();
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Obtener semilla real usada reportada por Forge
            long seedUsed = request.resolvedSeed();
            JsonNode infoNode = root.path("info");
            if (!infoNode.isMissingNode() && !infoNode.asText().isBlank()) {
                try {
                    JsonNode parsedInfo = objectMapper.readTree(infoNode.asText());
                    if (parsedInfo.has("seed")) {
                        seedUsed = parsedInfo.path("seed").asLong(seedUsed);
                    }
                } catch (Exception ignored) {}
            }

            long durationMillis = System.currentTimeMillis() - startTime;
            log.info("Imagen generada exitosamente en {} ms ({} bytes, seed: {})", durationMillis, imageBytes.length, seedUsed);

            return GeneratedImage.png(imageBytes, seedUsed, request.resolvedWidth(), request.resolvedHeight(), durationMillis);

        } catch (IOException e) {
            throw new ImageGenerationException("Error de conexión con Stable Diffusion Forge en " + sdApiUrl + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageGenerationException("Generación de imagen interrumpida", e);
        }
    }
}
