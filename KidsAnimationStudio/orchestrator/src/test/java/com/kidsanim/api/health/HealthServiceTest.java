package com.kidsanim.api.health;

import com.kidsanim.api.infrastructure.config.ExternalServicesProperties;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthServiceTest {

    private final ExternalServicesProperties props =
            new ExternalServicesProperties("http://gw:8090/", "http://comfy:8188", "http://ollama:11434", 1000);

    @Test
    void reportsUpWhenEverythingResponds() throws Exception {
        HttpProbe probe = mock(HttpProbe.class);
        when(probe.probe(anyString(), anyInt())).thenAnswer(inv -> ComponentHealth.up(inv.getArgument(0), 5, "HTTP 200"));

        HealthResponse response = new HealthService(healthyDataSource(), probe, props).check();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.components()).containsOnlyKeys("database", "aiGateway", "comfyui", "ollama");
        assertThat(response.components().get("aiGateway").target()).isEqualTo("http://gw:8090/health");
        assertThat(response.components().get("comfyui").target()).isEqualTo("http://comfy:8188/system_stats");
    }

    @Test
    void reportsDegradedWhenAnAiEngineIsDown() throws Exception {
        HttpProbe probe = mock(HttpProbe.class);
        when(probe.probe(anyString(), anyInt())).thenAnswer(inv -> ComponentHealth.up(inv.getArgument(0), 5, "HTTP 200"));
        when(probe.probe(eq("http://comfy:8188/system_stats"), anyInt()))
                .thenReturn(ComponentHealth.down("http://comfy:8188/system_stats", "No responde"));

        HealthResponse response = new HealthService(healthyDataSource(), probe, props).check();

        assertThat(response.status()).isEqualTo("DEGRADED");
    }

    @Test
    void reportsDownWhenDatabaseIsUnavailable() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("connection refused"));
        HttpProbe probe = mock(HttpProbe.class);
        when(probe.probe(anyString(), anyInt())).thenAnswer(inv -> ComponentHealth.up(inv.getArgument(0), 5, "HTTP 200"));

        HealthResponse response = new HealthService(ds, probe, props).check();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.components().get("database").detail()).contains("connection refused");
    }

    @Test
    void aggregateTreatsMissingDatabaseAsDown() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("ollama", ComponentHealth.up("x", 1, "ok"));
        assertThat(HealthService.aggregate(components)).isEqualTo("DOWN");
    }

    private DataSource healthyDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.isValid(anyInt())).thenReturn(true);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getURL()).thenReturn("jdbc:postgresql://localhost:5434/kidsdb");
        return ds;
    }
}
