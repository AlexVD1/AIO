package com.storyvideo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryTone;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.infrastructure.security.ApiKeyFilter;
import com.storyvideo.api.pipeline.dto.PipelineStage;
import com.storyvideo.api.pipeline.dto.PipelineStatusDto;
import com.storyvideo.api.pipeline.service.StoryPipelineExecutor;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PipelineController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StoryPipelineExecutor storyPipelineExecutor;

    @Test
    @DisplayName("POST /api/v1/pipeline/execute debe retornar 202 Accepted con Location header")
    void executePipeline_ShouldReturnAccepted() throws Exception {
        UUID trackingId = UUID.randomUUID();
        StoryGenerationRequestDto request = new StoryGenerationRequestDto(
                StoryGenre.HORROR,
                StoryTone.DARK,
                "Hotel Encantado",
                null,
                60,
                "es-MX",
                4,
                null,
                null
        );

        PipelineStatusDto initialStatus = new PipelineStatusDto(
                trackingId,
                "INITIALIZED",
                PipelineStage.NOT_STARTED,
                0,
                "Pipeline encolado para ejecución",
                null,
                null,
                List.of(),
                LocalDateTime.now(),
                null
        );

        when(storyPipelineExecutor.registerPipeline()).thenReturn(trackingId);
        when(storyPipelineExecutor.getStatus(trackingId)).thenReturn(Optional.of(initialStatus));
        when(storyPipelineExecutor.executeAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(initialStatus));

        mockMvc.perform(post("/api/v1/pipeline/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/pipeline/status/" + trackingId))
                .andExpect(jsonPath("$.storyId").value(trackingId.toString()))
                .andExpect(jsonPath("$.status").value("INITIALIZED"))
                .andExpect(jsonPath("$.progressPercent").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/pipeline/status/{id} debe retornar 200 OK con el estado actual")
    void getPipelineStatus_ShouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        PipelineStatusDto status = new PipelineStatusDto(
                id,
                "COMPLETED",
                PipelineStage.COMPLETED,
                100,
                "Video generado y exportado exitosamente",
                "http://localhost:8081/assets/video.mp4",
                "export_videos/Hotel_Encantado.mp4",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(storyPipelineExecutor.getStatus(id)).thenReturn(Optional.of(status));

        mockMvc.perform(get("/api/v1/pipeline/status/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storyId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentStage").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercent").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/pipeline/status/{id} cuando no existe debe retornar 404 Not Found")
    void getPipelineStatus_NotFound_ShouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(storyPipelineExecutor.getStatus(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/pipeline/status/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }
}
