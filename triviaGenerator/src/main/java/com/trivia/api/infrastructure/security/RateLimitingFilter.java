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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de limitación de tasa (Rate Limiting) en memoria basado en ventana deslizante por IP.
 *
 * Protege la API de saturación y ataques de denegación de servicio (DoS).
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final int maxRequestsPerMinute;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ClientWindow> clientWindows = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${trivia.security.rate-limit.max-requests:120}") int maxRequestsPerMinute,
            ObjectMapper objectMapper) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Exentar health checks internos
        if (request.getRequestURI().startsWith("/api/v1/health") || request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        long currentMinute = System.currentTimeMillis() / 60000;

        ClientWindow window = clientWindows.compute(clientIp, (ip, current) -> {
            if (current == null || current.minute != currentMinute) {
                return new ClientWindow(currentMinute, new AtomicInteger(1));
            }
            current.counter.incrementAndGet();
            return current;
        });

        if (window.counter.get() > maxRequestsPerMinute) {
            log.warn("Rate limit excedido para IP {}: {} peticiones en el minuto actual (máx {})",
                    clientIp, window.counter.get(), maxRequestsPerMinute);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Límite de peticiones por minuto superado. Intente de nuevo más tarde."
            );
            problem.setTitle("Demasiadas peticiones");
            problem.setType(URI.create("https://api.trivia.com/errors/rate-limit-exceeded"));
            problem.setProperty("maxPorMinuto", maxRequestsPerMinute);
            problem.setProperty("timestamp", Instant.now().toString());

            response.getWriter().write(objectMapper.writeValueAsString(problem));
            return;
        }

        // Limpieza periódica de IPs antiguas para evitar fuga de memoria
        if (clientWindows.size() > 5000) {
            clientWindows.entrySet().removeIf(e -> e.getValue().minute < currentMinute);
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class ClientWindow {
        final long minute;
        final AtomicInteger counter;

        ClientWindow(long minute, AtomicInteger counter) {
            this.minute = minute;
            this.counter = counter;
        }
    }
}
