package com.trivia.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyFilter — Tests unitarios de seguridad")
class ApiKeyFilterTest {

    @Mock
    private FilterChain filterChain;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Permite acceso libre si no hay API Key configurada (modo desarrollo)")
    void permiteAccesoSinConfigurarApiKey() throws ServletException, IOException {
        ApiKeyFilter filter = new ApiKeyFilter("", objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/trivias");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Permite peticiones GET públicas incluso si hay API Key configurada")
    void permiteGetPublicoConApiKeyConfigurada() throws ServletException, IOException {
        ApiKeyFilter filter = new ApiKeyFilter("secret-token", objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trivias/random");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rechaza con HTTP 401 peticiones POST sin cabecera X-API-KEY")
    void rechazaPostSinCabecera() throws ServletException, IOException {
        ApiKeyFilter filter = new ApiKeyFilter("secret-token", objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/trivias");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("unauthorized");
    }

    @Test
    @DisplayName("Permite peticiones POST con cabecera X-API-KEY válida")
    void permitePostConCabeceraValida() throws ServletException, IOException {
        ApiKeyFilter filter = new ApiKeyFilter("secret-token", objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/trivias");
        request.addHeader("X-API-KEY", "secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
