package com.kidsanim.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs de los servicios externos locales de los que depende el orquestador.
 */
@ConfigurationProperties(prefix = "kids.services")
public record ExternalServicesProperties(
        String aiGatewayUrl,
        String comfyuiUrl,
        String ollamaUrl,
        int probeTimeoutMs
) {
    public ExternalServicesProperties {
        aiGatewayUrl = stripTrailingSlash(aiGatewayUrl, "http://localhost:8090");
        comfyuiUrl = stripTrailingSlash(comfyuiUrl, "http://localhost:8188");
        ollamaUrl = stripTrailingSlash(ollamaUrl, "http://localhost:11434");
        probeTimeoutMs = probeTimeoutMs > 0 ? probeTimeoutMs : 2500;
    }

    private static String stripTrailingSlash(String value, String fallback) {
        String v = (value == null || value.isBlank()) ? fallback : value.trim();
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }
}
