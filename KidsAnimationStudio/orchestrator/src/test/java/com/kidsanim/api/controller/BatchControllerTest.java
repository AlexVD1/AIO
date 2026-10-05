package com.kidsanim.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.enums.BatchJobStatus;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.dto.BatchResponse;
import com.kidsanim.api.dto.CreateBatchRequest;
import com.kidsanim.api.dto.TopicBatchItem;
import com.kidsanim.api.service.BatchJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchJobService batchJobService;

    @Test
    void createBatchReturns202Accepted() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        CreateBatchRequest request = new CreateBatchRequest(
                seriesId,
                "Lote Matutino",
                List.of(new TopicBatchItem(EducationalTopicType.COUNTING, "1 al 5", "Contar", 60)),
                true
        );

        BatchResponse response = new BatchResponse(
                batchId,
                seriesId,
                "Lote Matutino",
                BatchJobStatus.PENDING,
                1,
                0,
                0,
                List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(batchJobService.createBatch(any())).thenReturn(response);
        doNothing().when(batchJobService).executeBatchAsync(any(), any());

        mockMvc.perform(post("/api/v1/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(batchId.toString()))
                .andExpect(jsonPath("$.name").value("Lote Matutino"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getBatchByIdReturns200() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        BatchResponse response = new BatchResponse(
                batchId,
                seriesId,
                "Lote Matutino",
                BatchJobStatus.IN_PROGRESS,
                5,
                2,
                0,
                List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(batchJobService.getBatch(batchId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/batches/{id}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(batchId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedCount").value(2));
    }

    @Test
    void cancelBatchReturns200() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        BatchResponse response = new BatchResponse(
                batchId,
                seriesId,
                "Lote Matutino",
                BatchJobStatus.CANCELLED,
                5,
                2,
                0,
                List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(batchJobService.cancelBatch(batchId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/batches/{id}/cancel", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(batchId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
