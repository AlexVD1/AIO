package com.trivia.api.controller;

import com.trivia.api.dto.VideoGenerationRequest;
import com.trivia.api.dto.VideoGenerationResponse;
import com.trivia.api.service.AssetStorageService;
import com.trivia.api.service.video.VideoFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("TriviaVideoController — Tests de integración HTTP")
class TriviaVideoControllerTest extends AbstractControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private AssetStorageService storageService;

    @Test
    @DisplayName("POST /api/v1/videos/generate → HTTP 201 Created cuando la petición es válida")
    void postGenerateVideo_validoRetorna201() {
        UUID videoId = UUID.randomUUID();
        VideoGenerationResponse mockResponse = new VideoGenerationResponse(
                videoId,
                "COMPLETED",
                "http://localhost:8080/assets/videos/" + videoId + ".mp4",
                2,
                26.0,
                VideoFormat.VERTICAL_9_16,
                LocalDateTime.now()
        );

        when(triviaVideoService.generarVideo(any())).thenReturn(mockResponse);

        VideoGenerationRequest req = new VideoGenerationRequest(
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                VideoFormat.VERTICAL_9_16,
                true
        );

        ResponseEntity<VideoGenerationResponse> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/videos/generate",
                req,
                VideoGenerationResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isEqualTo(videoId);
        assertThat(resp.getBody().status()).isEqualTo("COMPLETED");
        assertThat(resp.getBody().totalTrivias()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/v1/videos/generate → HTTP 400 Bad Request cuando la lista de trivias está vacía")
    void postGenerateVideo_listaVaciaRetorna400() {
        VideoGenerationRequest req = new VideoGenerationRequest(
                List.of(),
                VideoFormat.VERTICAL_9_16,
                true
        );

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/videos/generate",
                req,
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/v1/videos/intro-templates → HTTP 200 OK con lista de plantillas")
    void getIntroTemplates_retorna200() {
        when(triviaVideoService.getIntroTemplates()).thenReturn(List.of(
                new com.trivia.api.service.video.VideoIntroTemplate("tpl_1", "Pon a prueba tus conocimientos sobre {tema}.", "...")
        ));

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/videos/intro-templates",
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("tpl_1").contains("Pon a prueba");
    }

    @Test
    @DisplayName("POST /api/v1/videos/preview-intro → HTTP 200 OK con resultado resuelto")
    void postPreviewIntro_retorna200() {
        com.trivia.api.dto.VideoIntroPreviewRequest req = new com.trivia.api.dto.VideoIntroPreviewRequest(
                com.trivia.api.service.video.VideoIntroMode.TEMPLATE,
                "Interstellar",
                "¿Qué tanto sabes de {tema}?",
                null,
                List.of(),
                "es-MX"
        );

        when(triviaVideoService.previewIntro(any())).thenReturn(
                new com.trivia.api.dto.VideoIntroPreviewResponse(
                        com.trivia.api.service.video.VideoIntroMode.TEMPLATE,
                        "Interstellar",
                        "¿Qué tanto sabes de Interstellar?"
                )
        );

        ResponseEntity<com.trivia.api.dto.VideoIntroPreviewResponse> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/videos/preview-intro",
                req,
                com.trivia.api.dto.VideoIntroPreviewResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().topic()).isEqualTo("Interstellar");
        assertThat(resp.getBody().introText()).isEqualTo("¿Qué tanto sabes de Interstellar?");
    }
}
