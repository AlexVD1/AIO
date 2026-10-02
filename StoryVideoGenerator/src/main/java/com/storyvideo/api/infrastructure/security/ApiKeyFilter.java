package com.storyvideo.api.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String configuredApiKey;

    public ApiKeyFilter(@Value("${story.security.api-key:}") String configuredApiKey) {
        this.configuredApiKey = configuredApiKey != null ? configuredApiKey.trim() : "";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (configuredApiKey.isEmpty()) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Endpoints públicos exentos
        if (path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/api/v1/health")
                || path.startsWith("/assets/")) {
            return true;
        }

        // Consultas GET son de lectura pública
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }

        // Preflight OPTIONS de CORS
        return "OPTIONS".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestApiKey = request.getHeader("X-API-KEY");

        if (requestApiKey == null || !requestApiKey.equals(configuredApiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {
                      "type": "about:blank",
                      "title": "Unauthorized",
                      "status": 401,
                      "detail": "Cabecera X-API-KEY inválida o ausente"
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
