package com.trivia.api.controller;

import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.EstadoGeneracion;
import com.trivia.api.domain.EstadoTrivia;
import com.trivia.api.dto.StatsResponse;
import com.trivia.api.dto.TriviaGenerationRequest;
import com.trivia.api.dto.TriviaGenerationResponse;
import com.trivia.api.dto.TriviaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("TriviaController — Tests de integración HTTP")
class TriviaControllerTest extends AbstractControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private TriviaResponse crearTriviaResponseMock(UUID id) {
        return new TriviaResponse(
                id,
                "ASTRONOMIA",
                "Planetas",
                "¿Cuál es el planeta más grande?",
                "Júpiter es el más grande.",
                Dificultad.FACIL,
                "es-MX",
                EstadoTrivia.ACTIVA,
                List.of(),
                List.of(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/trivias con request válido devuelve HTTP 201 Created")
    void postTriviasValidoRetorna201() {
        TriviaGenerationRequest request = new TriviaGenerationRequest(
                "ASTRONOMIA", "Planetas", 5, 4, Dificultad.FACIL, "es-MX"
        );

        UUID genId = UUID.randomUUID();
        TriviaGenerationResponse mockResponse = new TriviaGenerationResponse(
                genId, "ASTRONOMIA", "Planetas", 5, 5, 0, 5, 4,
                Dificultad.FACIL, "es-MX", 1, EstadoGeneracion.COMPLETADA,
                null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(triviaGenerationService.generarTrivias(any())).thenReturn(mockResponse);

        ResponseEntity<TriviaGenerationResponse> response = restTemplate.postForEntity(
                url("/api/v1/trivias"), request, TriviaGenerationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(genId);
        assertThat(response.getBody().estado()).isEqualTo(EstadoGeneracion.COMPLETADA);
    }

    @Test
    @DisplayName("POST /api/v1/trivias con datos inválidos devuelve HTTP 400 Bad Request")
    void postTriviasInvalidoRetorna400() {
        // Cantidad negativa y opciones fuera de rango (menos de 2)
        TriviaGenerationRequest invalidRequest = new TriviaGenerationRequest(
                "", null, -1, 1, null, "invalido"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/trivias"), invalidRequest, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("validation-error");
    }

    @Test
    @DisplayName("GET /api/v1/trivias/{id} devuelve HTTP 200 si la trivia existe")
    void getTriviaPorIdExistenteRetorna200() {
        UUID id = UUID.randomUUID();
        when(triviaQueryService.obtenerPorId(id)).thenReturn(crearTriviaResponseMock(id));

        ResponseEntity<TriviaResponse> response = restTemplate.getForEntity(
                url("/api/v1/trivias/" + id), TriviaResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(id);
    }

    @Test
    @DisplayName("GET /api/v1/trivias/{id} devuelve HTTP 404 si la trivia no existe")
    void getTriviaPorIdInexistenteRetorna404() {
        UUID id = UUID.randomUUID();
        when(triviaQueryService.obtenerPorId(id))
                .thenThrow(new NoSuchElementException("Trivia no encontrada"));

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/v1/trivias/" + id), String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /api/v1/trivias/random devuelve HTTP 200 con trivia aleatoria")
    void getRandomTriviaRetorna200() {
        UUID id = UUID.randomUUID();
        when(triviaQueryService.obtenerAleatoria()).thenReturn(crearTriviaResponseMock(id));

        ResponseEntity<TriviaResponse> response = restTemplate.getForEntity(
                url("/api/v1/trivias/random"), TriviaResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(id);
    }

    @Test
    @DisplayName("GET /api/v1/trivias/stats devuelve HTTP 200 con estadísticas")
    void getStatsRetorna200() {
        StatsResponse stats = new StatsResponse(
                42L, 10L, 1.2, 0.5, Map.of("FACIL", 20L, "MEDIA", 15L, "DIFICIL", 7L)
        );
        when(triviaQueryService.obtenerEstadisticas()).thenReturn(stats);

        ResponseEntity<StatsResponse> response = restTemplate.getForEntity(
                url("/api/v1/trivias/stats"), StatsResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalTriviasActivas()).isEqualTo(42L);
    }

    @Test
    @DisplayName("GET /api/v1/trivias/search devuelve HTTP 200 con lista paginada")
    void getSearchRetorna200() {
        when(triviaQueryService.buscarPorTexto(any(), any()))
                .thenReturn(new PageImpl<>(List.of(crearTriviaResponseMock(UUID.randomUUID()))));

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/v1/trivias/search?q=planeta"), String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /api/v1/trivias/async devuelve HTTP 202 Accepted con estado PENDIENTE")
    void postAsyncRetorna202() {
        TriviaGenerationRequest request = new TriviaGenerationRequest(
                "ASTRONOMIA", "Galaxias", 3, 4, Dificultad.MEDIA, "es-MX"
        );

        UUID genId = UUID.randomUUID();
        TriviaGenerationResponse mockPendiente = new TriviaGenerationResponse(
                genId, "ASTRONOMIA", "Galaxias", 3, 0, 0, 0, 4,
                Dificultad.MEDIA, "es-MX", 0, EstadoGeneracion.PENDIENTE,
                null, LocalDateTime.now(), null
        );

        when(triviaGenerationService.registrarSolicitudPendiente(any())).thenReturn(mockPendiente);

        ResponseEntity<TriviaGenerationResponse> response = restTemplate.postForEntity(
                url("/api/v1/trivias/async"), request, TriviaGenerationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(genId);
        assertThat(response.getBody().estado()).isEqualTo(EstadoGeneracion.PENDIENTE);
    }

    @Test
    @DisplayName("GET /api/v1/trivias/generations/{id}/status devuelve HTTP 200 con estado en vivo")
    void getStatusRetorna200() {
        UUID genId = UUID.randomUUID();
        TriviaGenerationResponse mockStatus = new TriviaGenerationResponse(
                genId, "ASTRONOMIA", "Galaxias", 3, 4, 1, 3, 4,
                Dificultad.MEDIA, "es-MX", 1, EstadoGeneracion.COMPLETADA,
                null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(triviaGenerationService.obtenerEstadoGeneracion(genId)).thenReturn(mockStatus);

        ResponseEntity<TriviaGenerationResponse> response = restTemplate.getForEntity(
                url("/api/v1/trivias/generations/" + genId + "/status"), TriviaGenerationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(genId);
        assertThat(response.getBody().estado()).isEqualTo(EstadoGeneracion.COMPLETADA);
    }
}
