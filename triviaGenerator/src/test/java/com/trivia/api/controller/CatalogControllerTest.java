package com.trivia.api.controller;

import com.trivia.api.dto.TipoTriviaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests de integración para CatalogController.
 *
 * Extiende AbstractControllerTest que provee:
 *   - @SpringBootTest(RANDOM_PORT)
 *   - @MockBean CatalogService (accesible vía campo heredado 'catalogService')
 *
 * Cada test configura el comportamiento del mock con when(...).thenReturn(...)
 * y luego hace una llamada HTTP real para verificar el comportamiento del controller.
 */
@DisplayName("CatalogController — Tests de integración HTTP")
class CatalogControllerTest extends AbstractControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/v1/catalogos/tipos-trivia → HTTP 200")
    void debeRetornarHttp200() {
        when(catalogService.obtenerTiposActivos()).thenReturn(List.of());

        ResponseEntity<List<TipoTriviaResponse>> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/catalogos/tipos-trivia",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /api/v1/catalogos/tipos-trivia → retorna lista vacía cuando no hay tipos")
    void debeRetornarListaVaciaCorrectamente() {
        when(catalogService.obtenerTiposActivos()).thenReturn(List.of());

        ResponseEntity<List<TipoTriviaResponse>> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/catalogos/tipos-trivia",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getBody()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/catalogos/tipos-trivia → retorna campos correctos en cada tipo")
    void debeRetornarCamposCorrectosEnCadaTipo() {
        UUID id = UUID.randomUUID();
        TipoTriviaResponse astronomia = new TipoTriviaResponse(
                id, "ASTRONOMIA", "Astronomía",
                "Planetas, estrellas, galaxias y cosmología"
        );
        when(catalogService.obtenerTiposActivos()).thenReturn(List.of(astronomia));

        ResponseEntity<List<TipoTriviaResponse>> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/catalogos/tipos-trivia",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getBody()).hasSize(1);
        TipoTriviaResponse primero = resp.getBody().get(0);
        assertThat(primero.codigo()).isEqualTo("ASTRONOMIA");
        assertThat(primero.nombre()).isEqualTo("Astronomía");
        assertThat(primero.descripcion()).isNotBlank();
        assertThat(primero.id()).isEqualTo(id);
    }

    @Test
    @DisplayName("GET /api/v1/catalogos/tipos-trivia → retorna los 9 tipos del seed")
    void debeRetornarLosTiposDelSeed() {
        List<TipoTriviaResponse> tipos = List.of(
                new TipoTriviaResponse(UUID.randomUUID(), "ANIMALES",        "Animales",         "desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "ASTRONOMIA",      "Astronomía",       "desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "CIENCIA_NATURAL", "Ciencias Naturales","desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "CULTURA_GENERAL", "Cultura General",  "desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "GEOGRAFIA",       "Geografía",        "desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "GEOLOGIA",        "Geología",         "desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "HISTORIA",        "Historia",         "desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "MATEMATICAS",     "Matemáticas",      "desc"),
                new TipoTriviaResponse(UUID.randomUUID(), "TECNOLOGIA",      "Tecnología",       "desc")
        );
        when(catalogService.obtenerTiposActivos()).thenReturn(tipos);

        ResponseEntity<List<TipoTriviaResponse>> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/catalogos/tipos-trivia",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getBody()).hasSize(9);
        List<String> codigos = resp.getBody().stream()
                .map(TipoTriviaResponse::codigo).toList();
        assertThat(codigos).containsExactlyInAnyOrder(
                "ANIMALES", "ASTRONOMIA", "CIENCIA_NATURAL", "CULTURA_GENERAL",
                "GEOGRAFIA", "GEOLOGIA", "HISTORIA", "MATEMATICAS", "TECNOLOGIA"
        );
    }
}
