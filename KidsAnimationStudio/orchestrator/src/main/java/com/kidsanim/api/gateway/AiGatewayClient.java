package com.kidsanim.api.gateway;

import com.kidsanim.api.infrastructure.config.ExternalServicesProperties;
import com.kidsanim.api.infrastructure.exception.AiGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayClient.class);

    private final RestClient restClient;

    public AiGatewayClient(ExternalServicesProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(properties.aiGatewayUrl())
                .build();
    }

    public record SheetAiRequest(
            String prompt,
            String stylePrompt,
            String negativePrompt,
            Long seed,
            Integer width,
            Integer height,
            String outputDir,
            Integer count
    ) {}

    public record SheetAiResponse(
            List<String> images,
            List<Long> seeds
    ) {}

    public SheetAiResponse generateCharacterSheet(SheetAiRequest request) {
        log.info("Solicitando generación de hoja de personaje a AI Gateway: prompt='{}', count={}",
                request.prompt(), request.count());
        try {
            SheetAiResponse response = restClient.post()
                    .uri("/image/character-sheet")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SheetAiResponse.class);

            if (response == null || response.images() == null || response.images().isEmpty()) {
                throw new AiGatewayException("AI Gateway no devolvió imágenes válidas para la hoja de personaje");
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.error("Error devuelto por AI Gateway ({}): {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AiGatewayException("Error en AI Gateway al generar hoja de personaje: " + ex.getMessage(),
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Fallo de comunicación con AI Gateway: {}", ex.getMessage(), ex);
            throw new AiGatewayException("No se pudo comunicar con AI Gateway (" + ex.getMessage() + ")", ex);
        }
    }

    public record TtsAiRequest(
            String text,
            String voice,
            String rate,
            String pitch,
            String outputPath,
            String voiceEngine,
            String voiceReferencePath
    ) {
        public TtsAiRequest(String text, String voice, String rate, String pitch, String outputPath) {
            this(text, voice, rate, pitch, outputPath, "KOKORO", null);
        }
    }

    public record TtsAiResponse(
            String audioPath,
            int durationMs
    ) {}

    public record AlignAiRequest(
            String audioPath,
            String text,
            String language
    ) {}

    public record WordTimestamp(
            String word,
            int startMs,
            int endMs
    ) {}

    public record AlignAiResponse(
            List<WordTimestamp> words
    ) {}

    public TtsAiResponse generateTts(TtsAiRequest request) {
        log.info("Solicitando TTS a AI Gateway: voz='{}', rate='{}', texto='{}'",
                request.voice(), request.rate(), request.text() != null ? request.text().substring(0, Math.min(30, request.text().length())) : "");
        try {
            TtsAiResponse response = restClient.post()
                    .uri("/tts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TtsAiResponse.class);

            if (response == null || response.audioPath() == null || response.audioPath().isBlank()) {
                throw new AiGatewayException("AI Gateway no devolvió una ruta de audio válida en TTS");
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.error("Error devuelto por AI Gateway en /tts ({}): {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AiGatewayException("Error en AI Gateway al sintetizar voz: " + ex.getMessage(), ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Fallo de comunicación con AI Gateway en /tts: {}", ex.getMessage(), ex);
            throw new AiGatewayException("No se pudo comunicar con AI Gateway para TTS (" + ex.getMessage() + ")", ex);
        }
    }

    public AlignAiResponse alignAudio(AlignAiRequest request) {
        log.info("Solicitando alineación de timestamps a AI Gateway: audioPath='{}'", request.audioPath());
        try {
            AlignAiResponse response = restClient.post()
                    .uri("/align")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AlignAiResponse.class);

            if (response == null || response.words() == null) {
                return new AlignAiResponse(List.of());
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.error("Error devuelto por AI Gateway en /align ({}): {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AiGatewayException("Error en AI Gateway al alinear audio: " + ex.getMessage(), ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Fallo de comunicación con AI Gateway en /align: {}", ex.getMessage(), ex);
            throw new AiGatewayException("No se pudo comunicar con AI Gateway para alineación (" + ex.getMessage() + ")", ex);
        }
    }

    public record ReferenceImageSpec(
            String path,
            Double weight
    ) {}

    public record KeyframeAiRequest(
            String prompt,
            String negativePrompt,
            List<ReferenceImageSpec> referenceImages,
            Long seed,
            Integer width,
            Integer height,
            String outputPath
    ) {}

    public record KeyframeAiResponse(
            String imagePath,
            Long seed
    ) {}

    public record SimilarityAiRequest(
            String imagePath,
            String referencePath
    ) {}

    public record SimilarityAiResponse(
            Double score
    ) {}

    public record GpuFreeResponse(
            Integer freedMb
    ) {}

    public KeyframeAiResponse generateKeyframe(KeyframeAiRequest request) {
        log.info("Solicitando generación de keyframe a AI Gateway: prompt='{}', seed={}",
                request.prompt() != null ? request.prompt().substring(0, Math.min(30, request.prompt().length())) : "",
                request.seed());
        try {
            KeyframeAiResponse response = restClient.post()
                    .uri("/image/keyframe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(KeyframeAiResponse.class);

            if (response == null || response.imagePath() == null || response.imagePath().isBlank()) {
                throw new AiGatewayException("AI Gateway no devolvió una ruta válida para el keyframe");
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.error("Error devuelto por AI Gateway en /image/keyframe ({}): {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AiGatewayException("Error en AI Gateway al generar keyframe: " + ex.getMessage(), ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Fallo de comunicación con AI Gateway en /image/keyframe: {}", ex.getMessage(), ex);
            throw new AiGatewayException("No se pudo comunicar con AI Gateway para generar keyframe (" + ex.getMessage() + ")", ex);
        }
    }

    public SimilarityAiResponse calculateSimilarity(SimilarityAiRequest request) {
        log.info("Solicitando cálculo de similitud QA a AI Gateway: image='{}', ref='{}'",
                request.imagePath(), request.referencePath());
        try {
            SimilarityAiResponse response = restClient.post()
                    .uri("/qa/similarity")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SimilarityAiResponse.class);

            if (response == null || response.score() == null) {
                return new SimilarityAiResponse(0.0);
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.error("Error devuelto por AI Gateway en /qa/similarity ({}): {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AiGatewayException("Error en AI Gateway al calcular similitud: " + ex.getMessage(), ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Fallo de comunicación con AI Gateway en /qa/similarity: {}", ex.getMessage(), ex);
            throw new AiGatewayException("No se pudo comunicar con AI Gateway para similitud (" + ex.getMessage() + ")", ex);
        }
    }

    public GpuFreeResponse freeGpu() {
        log.info("Solicitando liberación de VRAM GPU a AI Gateway");
        try {
            GpuFreeResponse response = restClient.post()
                    .uri("/gpu/free")
                    .retrieve()
                    .body(GpuFreeResponse.class);

            if (response == null) {
                return new GpuFreeResponse(0);
            }
            return response;
        } catch (Exception ex) {
            log.warn("No se pudo liberar GPU tras etapa de procesamiento: {}", ex.getMessage());
            return new GpuFreeResponse(0);
        }
    }

    public record VideoI2VAiRequest(
            String model,
            String imagePath,
            String actionPrompt,
            String negativePrompt,
            Integer durationMs,
            Integer fps,
            Integer width,
            Integer height,
            Long seed,
            String outputPath
    ) {}

    public record VideoI2VAiResponse(
            String clipPath,
            String lastFramePath,
            int frames,
            int durationMs
    ) {}

    public VideoI2VAiResponse generateVideoI2V(VideoI2VAiRequest request) {
        log.info("Solicitando animación I2V a AI Gateway: model='{}', image='{}', dur={}ms, seed={}",
                request.model(), request.imagePath(), request.durationMs(), request.seed());
        try {
            VideoI2VAiResponse response = restClient.post()
                    .uri("/video/i2v")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(VideoI2VAiResponse.class);

            if (response == null || response.clipPath() == null || response.clipPath().isBlank()) {
                throw new AiGatewayException("AI Gateway no devolvió un clip de video válido");
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.error("Error devuelto por AI Gateway en /video/i2v ({}): {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AiGatewayException("Error en AI Gateway al generar video I2V: " + ex.getMessage(), ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Fallo de comunicación con AI Gateway en /video/i2v: {}", ex.getMessage(), ex);
            throw new AiGatewayException("No se pudo comunicar con AI Gateway para animación (" + ex.getMessage() + ")", ex);
        }
    }
}
