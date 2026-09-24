package com.trivia.api.controller;

import com.trivia.api.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración del endpoint GET /api/v1/health.
 *
 * Extiende AbstractControllerTest que provee:
 *   - @SpringBootTest(RANDOM_PORT) — servidor real en puerto aleatorio
 *   - @MockBean para todos los services con dependencias JPA
 *
 * Sin extender AbstractControllerTest, este test fallaría al intentar
 * crear CatalogController → CatalogService → TipoTriviaRepository
 * (JPA deshabilitado en tests por application.properties de test).
 */
@DisplayName("HealthController — Tests de integración")
class HealthControllerTest extends AbstractControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/v1/health devuelve HTTP 200")
    void healthEndpointReturns200() {
        ResponseEntity<HealthResponse> response = restTemplate
                .getForEntity("http://localhost:" + port + "/api/v1/health", HealthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /api/v1/health devuelve status=UP")
    void healthEndpointReturnsStatusUp() {
        ResponseEntity<HealthResponse> response = restTemplate
                .getForEntity("http://localhost:" + port + "/api/v1/health", HealthResponse.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("UP");
    }

    @Test
    @DisplayName("GET /api/v1/health devuelve nombre del servicio")
    void healthEndpointReturnsServiceName() {
        ResponseEntity<HealthResponse> response = restTemplate
                .getForEntity("http://localhost:" + port + "/api/v1/health", HealthResponse.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().servicio()).isEqualTo("trivia-api");
    }

    @Test
    @DisplayName("GET /api/v1/health incluye timestamp no nulo")
    void healthEndpointReturnsTimestamp() {
        ResponseEntity<HealthResponse> response = restTemplate
                .getForEntity("http://localhost:" + port + "/api/v1/health", HealthResponse.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotBlank();
    }
}
