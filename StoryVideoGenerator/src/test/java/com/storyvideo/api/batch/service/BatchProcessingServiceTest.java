package com.storyvideo.api.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.batch.dto.BatchGenerationRequestDto;
import com.storyvideo.api.batch.dto.BatchStatusResponseDto;
import com.storyvideo.api.domain.BatchJob;
import com.storyvideo.api.domain.BatchJobStatus;
import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.domain.StoryTone;
import com.storyvideo.api.pipeline.dto.PipelineStage;
import com.storyvideo.api.pipeline.dto.PipelineStatusDto;
import com.storyvideo.api.pipeline.service.StoryPipelineExecutor;
import com.storyvideo.api.repository.BatchJobRepository;
import com.storyvideo.api.repository.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchProcessingServiceTest {

    @Mock
    private BatchJobRepository batchJobRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StoryPipelineExecutor storyPipelineExecutor;

    private ObjectMapper objectMapper;
    private BatchProcessingService batchProcessingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        batchProcessingService = new BatchProcessingService(
                batchJobRepository,
                storyRepository,
                storyPipelineExecutor,
                objectMapper
        );
    }

    @Test
    @DisplayName("Debe crear y encolar un lote de historias exitosamente")
    void submitBatch_Success() {
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

        UUID batchId = UUID.randomUUID();
        when(batchJobRepository.save(any(BatchJob.class))).thenAnswer(invocation -> {
            BatchJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", batchId);
            return job;
        });
        when(batchJobRepository.findById(batchId)).thenAnswer(invocation -> {
            BatchJob job = new BatchJob(3, "{\"HORROR\":2,\"SCI_FI\":1}");
            ReflectionTestUtils.setField(job, "id", batchId);
            return Optional.of(job);
        });
        when(storyRepository.findByBatchJobIdOrderByCreatedAtAsc(batchId)).thenReturn(List.of());

        BatchStatusResponseDto response = batchProcessingService.submitBatch(request);

        assertThat(response).isNotNull();
        assertThat(response.batchId()).isEqualTo(batchId);
        assertThat(response.totalRequested()).isEqualTo(3);
        verify(batchJobRepository, atLeastOnce()).save(any(BatchJob.class));
    }

    @Test
    @DisplayName("Debe procesar lote y marcar COMPLETED cuando todas las historias son exitosas")
    void executeBatchAsync_AllSucceed_MarksCompleted() {
        UUID batchId = UUID.randomUUID();
        BatchJob batchJob = new BatchJob(2, "{\"HORROR\":2}");
        ReflectionTestUtils.setField(batchJob, "id", batchId);

        when(batchJobRepository.findById(batchId)).thenReturn(Optional.of(batchJob));
        when(storyPipelineExecutor.execute(any(), any(), any())).thenReturn(
                new PipelineStatusDto(UUID.randomUUID(), "COMPLETED", PipelineStage.COMPLETED, 100, "Éxito", null, null, List.of(), LocalDateTime.now(), LocalDateTime.now())
        );

        BatchGenerationRequestDto request = new BatchGenerationRequestDto(
                Map.of(StoryGenre.HORROR, 2),
                StoryTone.DARK,
                "es-MX",
                60,
                4,
                null,
                null,
                2
        );

        CompletableFuture<Void> future = batchProcessingService.executeBatchAsync(batchId, request);
        future.join();

        assertThat(batchJob.getStatus()).isEqualTo(BatchJobStatus.COMPLETED);
        assertThat(batchJob.getTotalCompleted()).isEqualTo(2);
        assertThat(batchJob.getTotalFailed()).isEqualTo(0);
        verify(batchJobRepository, atLeastOnce()).save(batchJob);
    }

    @Test
    @DisplayName("Aislamiento de errores: una historia fallida no detiene el lote y marca PARTIALLY_COMPLETED")
    void executeBatchAsync_ErrorIsolation_MarksPartiallyCompleted() {
        UUID batchId = UUID.randomUUID();
        BatchJob batchJob = new BatchJob(2, "{\"HORROR\":2}");
        ReflectionTestUtils.setField(batchJob, "id", batchId);

        when(batchJobRepository.findById(batchId)).thenReturn(Optional.of(batchJob));

        // Primera historia éxito, segunda historia fallo
        when(storyPipelineExecutor.execute(any(), any(), any()))
                .thenReturn(new PipelineStatusDto(UUID.randomUUID(), "COMPLETED", PipelineStage.COMPLETED, 100, "Éxito", null, null, List.of(), LocalDateTime.now(), LocalDateTime.now()))
                .thenReturn(new PipelineStatusDto(UUID.randomUUID(), "FAILED", PipelineStage.FAILED, 50, "Error de VRAM", null, null, List.of("Error"), LocalDateTime.now(), LocalDateTime.now()));

        BatchGenerationRequestDto request = new BatchGenerationRequestDto(
                Map.of(StoryGenre.HORROR, 2),
                StoryTone.DARK,
                "es-MX",
                60,
                4,
                null,
                null,
                1
        );

        CompletableFuture<Void> future = batchProcessingService.executeBatchAsync(batchId, request);
        future.join();

        assertThat(batchJob.getStatus()).isEqualTo(BatchJobStatus.PARTIALLY_COMPLETED);
        assertThat(batchJob.getTotalCompleted()).isEqualTo(1);
        assertThat(batchJob.getTotalFailed()).isEqualTo(1);
        verify(batchJobRepository, atLeastOnce()).save(batchJob);
    }

    @Test
    @DisplayName("Debe lanzar excepción si se consulta un lote que no existe")
    void getBatchStatus_NotFound_ThrowsException() {
        UUID batchId = UUID.randomUUID();
        when(batchJobRepository.findById(batchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchProcessingService.getBatchStatus(batchId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lote no encontrado");
    }

    @Test
    @DisplayName("Reintenta únicamente historias en estado FAILED del lote")
    void retryFailedStoriesAsync_RetriesOnlyFailed() {
        UUID batchId = UUID.randomUUID();
        BatchJob batchJob = new BatchJob(2, "{\"HORROR\":2}");
        ReflectionTestUtils.setField(batchJob, "id", batchId);
        batchJob.setTotalCompleted(1);
        batchJob.setTotalFailed(1);
        batchJob.setStatus(BatchJobStatus.PARTIALLY_COMPLETED);

        Story completedStory = new Story("Historia 1", StoryGenre.HORROR, "Tema 1");
        completedStory.setStatus(StoryStatus.EXPORTED);
        ReflectionTestUtils.setField(completedStory, "id", UUID.randomUUID());

        Story failedStory = new Story("Historia 2", StoryGenre.HORROR, "Tema 2");
        failedStory.setStatus(StoryStatus.FAILED);
        ReflectionTestUtils.setField(failedStory, "id", UUID.randomUUID());

        when(batchJobRepository.findById(batchId)).thenReturn(Optional.of(batchJob));
        when(storyRepository.findByBatchJobIdOrderByCreatedAtAsc(batchId)).thenReturn(List.of(completedStory, failedStory));
        when(storyPipelineExecutor.executeExistingStory(any(), eq(failedStory.getId()))).thenReturn(
                new PipelineStatusDto(failedStory.getId(), "COMPLETED", PipelineStage.COMPLETED, 100, "Éxito en retry", null, null, List.of(), LocalDateTime.now(), LocalDateTime.now())
        );

        CompletableFuture<BatchStatusResponseDto> future = batchProcessingService.retryFailedStoriesAsync(batchId);
        BatchStatusResponseDto response = future.join();

        assertThat(response).isNotNull();
        assertThat(batchJob.getStatus()).isEqualTo(BatchJobStatus.COMPLETED);
        assertThat(batchJob.getTotalCompleted()).isEqualTo(2);
        assertThat(batchJob.getTotalFailed()).isEqualTo(0);

        verify(storyPipelineExecutor, times(1)).executeExistingStory(any(), eq(failedStory.getId()));
        verify(storyPipelineExecutor, never()).executeExistingStory(any(), eq(completedStory.getId()));
    }
}
