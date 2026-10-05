package com.kidsanim.api.health;

import com.kidsanim.api.infrastructure.config.ExternalServicesProperties;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agrega el estado de la DB propia (kidsdb) y de los motores locales de IA.
 */
@Service
public class HealthService {

    static final String SERVICE_NAME = "kids-animation-api";

    private final DataSource dataSource;
    private final HttpProbe httpProbe;
    private final ExternalServicesProperties services;

    public HealthService(DataSource dataSource, HttpProbe httpProbe, ExternalServicesProperties services) {
        this.dataSource = dataSource;
        this.httpProbe = httpProbe;
        this.services = services;
    }

    public HealthResponse check() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", checkDatabase());
        components.put("aiGateway", httpProbe.probe(services.aiGatewayUrl() + "/health", services.probeTimeoutMs()));
        components.put("comfyui", httpProbe.probe(services.comfyuiUrl() + "/system_stats", services.probeTimeoutMs()));
        components.put("ollama", httpProbe.probe(services.ollamaUrl() + "/api/tags", services.probeTimeoutMs()));

        return new HealthResponse(aggregate(components), SERVICE_NAME, OffsetDateTime.now(), components);
    }

    static String aggregate(Map<String, ComponentHealth> components) {
        ComponentHealth db = components.get("database");
        if (db == null || !db.isUp()) {
            return "DOWN";
        }
        boolean allUp = components.values().stream().allMatch(ComponentHealth::isUp);
        return allUp ? "UP" : "DEGRADED";
    }

    private ComponentHealth checkDatabase() {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            long latency = (System.nanoTime() - start) / 1_000_000;
            String url = connection.getMetaData().getURL();
            return valid ? ComponentHealth.up(url, latency, "Conexión válida")
                         : ComponentHealth.down(url, "Conexión inválida");
        } catch (Exception e) {
            return ComponentHealth.down("datasource", "Sin conexión: " + e.getMessage());
        }
    }
}
