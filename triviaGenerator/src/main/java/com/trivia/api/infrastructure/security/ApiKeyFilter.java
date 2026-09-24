package com.trivia.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Filtro de autenticación por API Key (cabecera 'X-API-KEY').
 *
 * REGLAS:
 *   - Si 'trivia.security.api-key' no está configurada (en blanco), se permite el acceso libre (modo dev).
 *   - Las consultas de solo lectura (GET, OPTIONS) y endpoints de salud/docs son públicos.
 *   - Las operaciones de modificación/generación (POST) exigen 'X-API-KEY' válida si está configurada.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-KEY";

    private final String configuredApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(
            @Value("${trivia.security.api-key:}") String configuredApiKey,
            ObjectMapper objectMapper) {
        this.configuredApiKey = configuredApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Si no hay API Key configurada, el filtro es no-op (desarrollo local)
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Endpoints públicos que no requieren autenticación
        if (isPublicEndpoint(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader(API_KEY_HEADER);

        if (providedApiKey == null || !providedApiKey.equals(configuredApiKey)) {
            log.warn("Acceso no autorizado rechazado en [{}] {} desde IP {}",
                    method, path, request.getRemoteAddr());

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "La cabecera 'X-API-KEY' es obligatoria y debe ser válida para esta operación."
            );
            problem.setTitle("No autorizado");
            problem.setType(URI.create("https://api.trivia.com/errors/unauthorized"));
            problem.setProperty("timestamp", Instant.now().toString());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(problem));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Métodos de lectura y preflight son públicos
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Endpoints de infraestructura públicos
        return path.startsWith("/api/v1/health")
                || path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/assets");
    }
}
