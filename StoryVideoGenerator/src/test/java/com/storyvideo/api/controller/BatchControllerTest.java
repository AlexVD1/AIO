package com.storyvideo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.batch.dto.BatchGenerationRequestDto;
import com.storyvideo.api.batch.dto.BatchStatusResponseDto;
import com.storyvideo.api.batch.service.BatchProcessingService;
import com.storyvideo.api.domain.BatchJobStatus;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryTone;
import com.storyvideo.api.infrastructure.security.ApiKeyFilter;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BatchController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchProcessingService batchProcessingService;

    @Test
    @DisplayName("POST /api/v1/stories/batch debe retornar 202 Accepted con Location header")
    void createBatch_ShouldReturnAccepted() throws Exception {
        UUID batchId = UUID.randomUUID();
        BatchGenerationRequestDto request = new BatchGenerationRequestDto(
                Map.of(StoryGenre.HORROR, 2, StoryGenre.SCI_FI, 1),
                StoryTone.DARK,
                "es-MX",
                60,
                4,
                null,
                null,
                1
        );

        BatchStatusResponseDto initialResponse = new BatchStatusResponseDto(
                batchId,
                BatchJobStatus.CREATED,
                3,
                0,
                0,
                0,
                Map.of("HORROR", 2, "SCI_FI", 1),
                List.of(),
                LocalDateTime.now(),
                null
        );

        when(batchProcessingService.submitBatch(any())).thenReturn(initialResponse);

        mockMvc.perform(post("/api/v1/stories/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/batches/" + batchId + "/status"))
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalRequested").value(3))
                .andExpect(jsonPath("$.progressPercent").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/batches/{id}/status debe retornar 200 OK con el estado del lote")
    void getBatchStatus_ShouldReturnOk() throws Exception {
        UUID batchId = UUID.randomUUID();
        BatchStatusResponseDto response = new BatchStatusResponseDto(
                batchId,
                BatchJobStatus.PROCESSING,
                5,
                3,
                1,
                80,
                Map.of("HORROR", 5),
                List.of(),
                LocalDateTime.now(),
                null
        );

        when(batchProcessingService.getBatchStatus(batchId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/batches/" + batchId + "/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.totalRequested").value(5))
                .andExpect(jsonPath("$.totalCompleted").value(3))
                .andExpect(jsonPath("$.totalFailed").value(1))
                .andExpect(jsonPath("$.progressPercent").value(80));
    }

    @Test
    @DisplayName("POST /api/v1/batches/{id}/retry-failed debe retornar 202 Accepted")
    void retryFailedStories_ShouldReturnAccepted() throws Exception {
        UUID batchId = UUID.randomUUID();
        BatchStatusResponseDto status = new BatchStatusResponseDto(
                batchId,
                BatchJobStatus.PROCESSING,
                5,
                3,
                0,
                60,
                Map.of("HORROR", 5),
                List.of(),
                LocalDateTime.now(),
                null
        );

        when(batchProcessingService.retryFailedStoriesAsync(batchId)).thenReturn(CompletableFuture.completedFuture(status));
        when(batchProcessingService.getBatchStatus(batchId)).thenReturn(status);

        mockMvc.perform(post("/api/v1/batches/" + batchId + "/retry-failed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/batches/" + batchId + "/status"))
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }
}
