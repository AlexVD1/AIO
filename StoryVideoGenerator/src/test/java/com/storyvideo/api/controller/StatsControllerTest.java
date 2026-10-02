package com.storyvideo.api.controller;

import com.storyvideo.api.export.service.AutoCleanupService;
import com.storyvideo.api.infrastructure.security.ApiKeyFilter;
import com.storyvideo.api.repository.BatchJobRepository;
import com.storyvideo.api.repository.StoryRepository;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoryRepository storyRepository;

    @MockBean
    private BatchJobRepository batchJobRepository;

    @MockBean
    private AutoCleanupService autoCleanupService;

    @Test
    @DisplayName("GET /api/v1/stats debe retornar métricas del sistema y espacio en disco")
    void getStats_ShouldReturnSystemMetrics() throws Exception {
        when(storyRepository.count()).thenReturn(15L);
        when(batchJobRepository.count()).thenReturn(2L);
        when(autoCleanupService.checkDiskSpace()).thenReturn(45000L);

        mockMvc.perform(get("/api/v1/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStories").value(15))
                .andExpect(jsonPath("$.totalBatches").value(2))
                .andExpect(jsonPath("$.usableDiskSpaceMb").value(45000))
                .andExpect(jsonPath("$.serviceStatus").value("OPERATIONAL"));
    }
}
