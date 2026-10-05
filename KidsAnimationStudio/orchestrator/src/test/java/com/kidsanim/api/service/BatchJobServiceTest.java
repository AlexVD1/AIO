package com.kidsanim.api.service;

import com.kidsanim.api.domain.BatchJob;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.enums.BatchJobStatus;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.dto.BatchResponse;
import com.kidsanim.api.dto.CreateBatchRequest;
import com.kidsanim.api.dto.EpisodePipelineStatusResponse;
import com.kidsanim.api.dto.TopicBatchItem;
import com.kidsanim.api.pipeline.EpisodePipelineExecutor;
import com.kidsanim.api.repository.BatchJobRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.SeriesRepository;
import com.kidsanim.api.script.EpisodeScriptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BatchJobServiceTest {

    private BatchJobRepository batchJobRepository;
    private SeriesRepository seriesRepository;
    private EpisodeRepository episodeRepository;
    private EpisodeScriptService episodeScriptService;
    private EpisodePipelineExecutor episodePipelineExecutor;
    private BatchJobService batchJobService;

    @BeforeEach
    void setUp() {
        batchJobRepository = mock(BatchJobRepository.class);
        seriesRepository = mock(SeriesRepository.class);
        episodeRepository = mock(EpisodeRepository.class);
        episodeScriptService = mock(EpisodeScriptService.class);
        episodePipelineExecutor = mock(EpisodePipelineExecutor.class);

        batchJobService = new BatchJobService(
                batchJobRepository,
                seriesRepository,
                episodeRepository,
                episodeScriptService,
                episodePipelineExecutor
        );
    }

    @Test
    void createBatchSuccess() {
        UUID seriesId = UUID.randomUUID();
        Series series = new Series("Tito el zorrito", "Aventuras", null);
        series.setId(seriesId);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(batchJobRepository.save(any(BatchJob.class))).thenAnswer(invocation -> {
            BatchJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        CreateBatchRequest request = new CreateBatchRequest(
                seriesId,
                "Lote de Números",
                List.of(
                        new TopicBatchItem(EducationalTopicType.COUNTING, "1 al 5", "Aprender del 1 al 5", 60),
                        new TopicBatchItem(EducationalTopicType.COLORS, "Colores primarios", "Aprender colores", 60)
                ),
                true
        );

        BatchResponse response = batchJobService.createBatch(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Lote de Números");
        assertThat(response.seriesId()).isEqualTo(seriesId);
        assertThat(response.requestedCount()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(BatchJobStatus.PENDING);
        verify(batchJobRepository).save(any(BatchJob.class));
    }

    @Test
    void cancelBatchSetsCancelledStatus() {
        UUID batchId = UUID.randomUUID();
        BatchJob batchJob = new BatchJob("Lote a cancelar", null, 5);
        batchJob.setId(batchId);
        batchJob.setStatus(BatchJobStatus.IN_PROGRESS);

        when(batchJobRepository.findById(batchId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchResponse response = batchJobService.cancelBatch(batchId);

        assertThat(response.status()).isEqualTo(BatchJobStatus.CANCELLED);
        verify(batchJobRepository).save(batchJob);
    }

    @Test
    void executeBatchAsyncProcessesTopicsSequentially() {
        UUID batchId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        Series series = new Series("Tito el zorrito", "Aventuras", null);
        series.setId(seriesId);

        BatchJob batchJob = new BatchJob("Lote prueba", series, 1);
        batchJob.setId(batchId);
        batchJob.setStatus(BatchJobStatus.PENDING);

        when(batchJobRepository.findById(batchId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Episode fakeEp = new Episode(series, EducationalTopicType.COUNTING, "1 al 3", "Contar", "Ep 1");
        fakeEp.setId(UUID.randomUUID());

        when(episodeScriptService.generateAndSaveEpisode(eq(seriesId), any())).thenReturn(fakeEp);
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID trackingId = UUID.randomUUID();
        when(episodePipelineExecutor.registerPipeline(any(), any())).thenReturn(trackingId);
        when(episodePipelineExecutor.executePipeline(eq(trackingId), any(), eq(true))).thenReturn(
                new EpisodePipelineStatusResponse(
                        fakeEp.getId(), trackingId, "Ep 1", "COMPLETED", "COMPLETED",
                        100, "Listo", "/path/vid.mp4", null, null, null
                )
        );

        CreateBatchRequest request = new CreateBatchRequest(
                seriesId,
                "Lote prueba",
                List.of(new TopicBatchItem(EducationalTopicType.COUNTING, "1 al 3", "Contar", 60)),
                true
        );

        batchJobService.executeBatchAsync(batchId, request);

        assertThat(batchJob.getCompletedCount()).isEqualTo(1);
        assertThat(batchJob.getFailedCount()).isEqualTo(0);
        assertThat(batchJob.getStatus()).isEqualTo(BatchJobStatus.COMPLETED);
        verify(episodePipelineExecutor).executePipeline(eq(trackingId), eq(fakeEp.getId()), eq(true));
    }
}
