package com.kidsanim.api.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

    @Test
    void returns200WhenDegraded() throws Exception {
        when(healthService.check()).thenReturn(new HealthResponse("DEGRADED", "kids-animation-api", OffsetDateTime.now(),
                Map.of("database", ComponentHealth.up("db", 1, "ok"), "comfyui", ComponentHealth.down("c", "off"))));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.components.comfyui.status").value("DOWN"));
    }

    @Test
    void returns503WhenDatabaseDown() throws Exception {
        when(healthService.check()).thenReturn(new HealthResponse("DOWN", "kids-animation-api", OffsetDateTime.now(),
                Map.of("database", ComponentHealth.down("db", "refused"))));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
