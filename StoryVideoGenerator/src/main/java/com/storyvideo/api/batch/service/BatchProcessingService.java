package com.storyvideo.api.batch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.batch.dto.BatchGenerationRequestDto;
import com.storyvideo.api.batch.dto.BatchStatusResponseDto;
import com.storyvideo.api.batch.dto.BatchStoryItemDto;
import com.storyvideo.api.domain.BatchJob;
import com.storyvideo.api.domain.BatchJobStatus;
import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.pipeline.dto.PipelineStatusDto;
import com.storyvideo.api.pipeline.service.StoryPipelineExecutor;
import com.storyvideo.api.repository.BatchJobRepository;
import com.storyvideo.api.repository.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BatchProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessingService.class);

    private final BatchJobRepository batchJobRepository;
    private final StoryRepository storyRepository;
    private final StoryPipelineExecutor storyPipelineExecutor;
    private final ObjectMapper objectMapper;

    public BatchProcessingService(
            BatchJobRepository batchJobRepository,
            StoryRepository storyRepository,
            StoryPipelineExecutor storyPipelineExecutor,
            ObjectMapper objectMapper) {
        this.batchJobRepository = batchJobRepository;
        this.storyRepository = storyRepository;
        this.storyPipelineExecutor = storyPipelineExecutor;
        this.objectMapper = objectMapper;
    }

    public BatchStatusResponseDto submitBatch(BatchGenerationRequestDto request) {
        int total = request.totalStories();
        if (total <= 0) {
            throw new IllegalArgumentException("El lote debe contener al menos 1 historia.");
        }
        if (total > 50) {
            throw new IllegalArgumentException("El tamaño máximo permitido para un lote es de 50 historias.");
        }

        String distributionJson = serializeDistribution(request.distribution());
        BatchJob batchJob = new BatchJob(total, distributionJson);
        batchJob = batchJobRepository.save(batchJob);

        log.info("Lote creado con ID: {} para un total de {} historias", batchJob.getId(), total);

        executeBatchAsync(batchJob.getId(), request);

        return getBatchStatus(batchJob.getId());
    }

    @Async("batchTaskExecutor")
    public CompletableFuture<Void> executeBatchAsync(UUID batchJobId, BatchGenerationRequestDto request) {
        BatchJob batchJob = batchJobRepository.findById(batchJobId).orElse(null);
        if (batchJob == null) {
            log.error("No se encontró el lote {} para iniciar procesamiento", batchJobId);
            return CompletableFuture.completedFuture(null);
        }

        batchJob.setStatus(BatchJobStatus.PROCESSING);
        batchJobRepository.save(batchJob);

        List<StoryGenerationRequestDto> storyRequests = flattenDistribution(request);
        int concurrency = request.resolvedMaxConcurrency();
        log.info("Iniciando procesamiento de lote {} con {} historias y concurrencia {}",
                batchJobId, storyRequests.size(), concurrency);

        Semaphore semaphore = new Semaphore(concurrency);
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (StoryGenerationRequestDto storyReq : storyRequests) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        BatchJob currentBatch = batchJobRepository.findById(batchJobId).orElse(null);
                        UUID trackingId = UUID.randomUUID();
                        PipelineStatusDto result = storyPipelineExecutor.execute(trackingId, storyReq, currentBatch);
                        if ("COMPLETED".equalsIgnoreCase(result.status())) {
                            completedCount.incrementAndGet();
                            log.info("Historia completada exitosamente en lote {}", batchJobId);
                        } else {
                            failedCount.incrementAndGet();
                            log.warn("Historia falló en lote {}: {}", batchJobId, result.message());
                        }
                    } finally {
                        semaphore.release();
                        updateBatchCounts(batchJobId, completedCount.get(), failedCount.get());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedCount.incrementAndGet();
                    log.error("Hilo de lote interrumpido", e);
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.error("Excepción inesperada procesando historia en lote: {}", e.getMessage(), e);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        finalizeBatch(batchJobId, completedCount.get(), failedCount.get(), request.totalStories());
        return CompletableFuture.completedFuture(null);
    }

    public BatchStatusResponseDto getBatchStatus(UUID batchJobId) {
        BatchJob batchJob = batchJobRepository.findById(batchJobId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado con ID: " + batchJobId));

        List<Story> stories = storyRepository.findByBatchJobIdOrderByCreatedAtAsc(batchJobId);
        List<BatchStoryItemDto> storyItems = stories.stream()
                .map(s -> new BatchStoryItemDto(
                        s.getId(),
                        s.getTitle(),
                        s.getGenre(),
                        s.getStatus(),
                        null,
                        null,
                        s.getCreatedAt(),
                        s.getUpdatedAt()
                ))
                .toList();

        int totalDone = batchJob.getTotalCompleted() + batchJob.getTotalFailed();
        int progress = batchJob.getTotalRequested() > 0
                ? (totalDone * 100 / batchJob.getTotalRequested())
                : 0;

        return new BatchStatusResponseDto(
                batchJob.getId(),
                batchJob.getStatus(),
                batchJob.getTotalRequested(),
                batchJob.getTotalCompleted(),
                batchJob.getTotalFailed(),
                progress,
                parseDistribution(batchJob.getGenreDistributionJson()),
                storyItems,
                batchJob.getCreatedAt(),
                batchJob.getCompletedAt()
        );
    }

    @Async("batchTaskExecutor")
    public CompletableFuture<BatchStatusResponseDto> retryFailedStoriesAsync(UUID batchJobId) {
        BatchJob batchJob = batchJobRepository.findById(batchJobId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado con ID: " + batchJobId));

        List<Story> failedStories = storyRepository.findByBatchJobIdOrderByCreatedAtAsc(batchJobId)
                .stream()
                .filter(s -> s.getStatus() == StoryStatus.FAILED)
                .toList();

        if (failedStories.isEmpty()) {
            log.info("No hay historias en estado FAILED para reintentar en el lote {}", batchJobId);
            return CompletableFuture.completedFuture(getBatchStatus(batchJobId));
        }

        batchJob.setStatus(BatchJobStatus.PROCESSING);
        batchJobRepository.save(batchJob);

        log.info("Reintentando {} historias fallidas en el lote {}", failedStories.size(), batchJobId);

        for (Story failedStory : failedStories) {
            try {
                UUID trackingId = UUID.randomUUID();
                PipelineStatusDto status = storyPipelineExecutor.executeExistingStory(trackingId, failedStory.getId());
                if ("COMPLETED".equalsIgnoreCase(status.status())) {
                    batchJob.setTotalCompleted(batchJob.getTotalCompleted() + 1);
                    batchJob.setTotalFailed(Math.max(0, batchJob.getTotalFailed() - 1));
                    batchJobRepository.save(batchJob);
                }
            } catch (Exception e) {
                log.error("Error al reintentar historia {}: {}", failedStory.getId(), e.getMessage());
            }
        }

        finalizeBatch(batchJobId, batchJob.getTotalCompleted(), batchJob.getTotalFailed(), batchJob.getTotalRequested());
        return CompletableFuture.completedFuture(getBatchStatus(batchJobId));
    }

    private synchronized void updateBatchCounts(UUID batchJobId, int completed, int failed) {
        try {
            BatchJob batch = batchJobRepository.findById(batchJobId).orElse(null);
            if (batch != null) {
                batch.setTotalCompleted(completed);
                batch.setTotalFailed(failed);
                batchJobRepository.save(batch);
            }
        } catch (Exception e) {
            log.warn("No se pudo actualizar progreso intermedio del lote {}: {}", batchJobId, e.getMessage());
        }
    }

    private void finalizeBatch(UUID batchJobId, int completed, int failed, int totalRequested) {
        BatchJob batch = batchJobRepository.findById(batchJobId).orElse(null);
        if (batch != null) {
            batch.setTotalCompleted(completed);
            batch.setTotalFailed(failed);
            batch.setCompletedAt(LocalDateTime.now());

            if (completed == totalRequested) {
                batch.setStatus(BatchJobStatus.COMPLETED);
            } else if (completed > 0) {
                batch.setStatus(BatchJobStatus.PARTIALLY_COMPLETED);
            } else {
                batch.setStatus(BatchJobStatus.FAILED);
            }

            batchJobRepository.save(batch);
            log.info("Lote {} finalizado con estado {} (Completadas: {}, Fallidas: {}, Solicitadas: {})",
                    batchJobId, batch.getStatus(), completed, failed, totalRequested);
        }
    }

    private List<StoryGenerationRequestDto> flattenDistribution(BatchGenerationRequestDto request) {
        List<StoryGenerationRequestDto> list = new ArrayList<>();
        if (request.distribution() == null) {
            return list;
        }

        for (Map.Entry<StoryGenre, Integer> entry : request.distribution().entrySet()) {
            StoryGenre genre = entry.getKey();
            int count = (entry.getValue() != null && entry.getValue() > 0) ? entry.getValue() : 0;
            for (int i = 0; i < count; i++) {
                list.add(new StoryGenerationRequestDto(
                        genre,
                        request.resolvedTone(),
                        null,
                        null,
                        request.resolvedTargetDurationSeconds(),
                        request.resolvedLanguage(),
                        request.resolvedSceneCount(),
                        request.visualStyleName(),
                        request.ttsVoice()
                ));
            }
        }
        return list;
    }

    private String serializeDistribution(Map<StoryGenre, Integer> distribution) {
        try {
            Map<String, Integer> stringMap = new LinkedHashMap<>();
            if (distribution != null) {
                distribution.forEach((k, v) -> stringMap.put(k.name(), v));
            }
            return objectMapper.writeValueAsString(stringMap);
        } catch (Exception e) {
            log.warn("Error serializando distribución de lote a JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private Map<String, Integer> parseDistribution(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            log.warn("Error deserializando distribución de lote desde JSON: {}", e.getMessage());
            return Map.of();
        }
    }
}
