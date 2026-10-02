package com.storyvideo.api.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record HealthResponse(
        String status,
        String service,
        String version,
        LocalDateTime timestamp,
        Map<String, Object> details
) {
    public static HealthResponse ok(String service, String version, Map<String, Object> details) {
        return new HealthResponse("UP", service, version, LocalDateTime.now(), details);
    }
}
