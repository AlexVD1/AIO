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
}
