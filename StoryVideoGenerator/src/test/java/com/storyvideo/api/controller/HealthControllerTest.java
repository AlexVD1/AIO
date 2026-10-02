package com.storyvideo.api.controller;

import com.storyvideo.api.infrastructure.security.ApiKeyFilter;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetStorageService storageService;

    @Test
    @DisplayName("GET /api/v1/health debe retornar HTTP 200 con estado UP y detalles")
    void checkHealth_ShouldReturnUp() throws Exception {
        when(storageService.resolveAbsolutePath(anyString())).thenReturn(Paths.get("."));

        mockMvc.perform(get("/api/v1/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("story-video-api"))
                .andExpect(jsonPath("$.details.imageProvider").value("stable-diffusion-local"))
                .andExpect(jsonPath("$.details.sdApiUrl").value("http://localhost:7860"));
    }
}
