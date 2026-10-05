package com.kidsanim.api.service;

import com.kidsanim.api.domain.BatchJob;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.enums.BatchJobStatus;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.dto.*;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.pipeline.EpisodePipelineExecutor;
import com.kidsanim.api.repository.BatchJobRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.SeriesRepository;
import com.kidsanim.api.script.EpisodeScriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BatchJobService {

    private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);

    private final BatchJobRepository batchJobRepository;
    private final SeriesRepository seriesRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeScriptService episodeScriptService;
    private final EpisodePipelineExecutor episodePipelineExecutor;

    public BatchJobService(
            BatchJobRepository batchJobRepository,
            SeriesRepository seriesRepository,
            EpisodeRepository episodeRepository,
            EpisodeScriptService episodeScriptService,
            EpisodePipelineExecutor episodePipelineExecutor) {
        this.batchJobRepository = batchJobRepository;
        this.seriesRepository = seriesRepository;
        this.episodeRepository = episodeRepository;
        this.episodeScriptService = episodeScriptService;
        this.episodePipelineExecutor = episodePipelineExecutor;
    }

    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request) {
        Series series = seriesRepository.findById(request.seriesId())
                .orElseThrow(() -> new ResourceNotFoundException("Serie no encontrada con ID: " + request.seriesId()));

        BatchJob batchJob = new BatchJob(request.name(), series, request.topics().size());
        batchJob.setStatus(BatchJobStatus.PENDING);
        BatchJob saved = batchJobRepository.save(batchJob);

        log.info("Lote de producción '{}' creado con {} episodios solicitados para serie '{}'",
                saved.getName(), saved.getRequestedCount(), series.getName());

        return BatchResponse.fromEntity(saved);
    }

    @Async("kidsPipelineExecutor")
    public void executeBatchAsync(UUID batchJobId, CreateBatchRequest request) {
        log.info("Iniciando procesamiento asíncrono para lote {}", batchJobId);
        BatchJob batchJob = batchJobRepository.findById(batchJobId).orElse(null);
        if (batchJob == null) return;

        batchJob.setStatus(BatchJobStatus.IN_PROGRESS);
        batchJob = batchJobRepository.save(batchJob);

        Series series = batchJob.getSeries();
        boolean autoApprove = request.autoApprove() != null ? request.autoApprove() : true;

        for (int i = 0; i < request.topics().size(); i++) {
            // Verificar si fue cancelado
            BatchJob current = batchJobRepository.findById(batchJobId).orElse(null);
            if (current != null && current.getStatus() == BatchJobStatus.CANCELLED) {
                log.warn("El lote {} fue cancelado por el usuario. Deteniendo ejecución.", batchJobId);
                break;
            }

            TopicBatchItem topic = request.topics().get(i);
            log.info("Procesando episodio {}/{} del lote {}: tema={}", i + 1, request.topics().size(), batchJobId, topic.topicType());

            try {
                CreateEpisodeRequest epReq = new CreateEpisodeRequest(
                        topic.topicType(),
                        topic.topicDetail(),
                        topic.learningObjective(),
                        topic.targetDurationSec(),
                        autoApprove
                );

                Episode episode = episodeScriptService.generateAndSaveEpisode(series.getId(), epReq);
                episode.setBatchJob(batchJob);
                episode = episodeRepository.save(episode);

                batchJob.getEpisodes().add(episode);

                UUID trackingId = episodePipelineExecutor.registerPipeline(episode.getId(), episode.getTitle());
                EpisodePipelineStatusResponse status = episodePipelineExecutor.executePipeline(trackingId, episode.getId(), autoApprove);

                if ("COMPLETED".equalsIgnoreCase(status.status())) {
                    batchJob.setCompletedCount(batchJob.getCompletedCount() + 1);
                } else {
                    batchJob.setFailedCount(batchJob.getFailedCount() + 1);
                }
            } catch (Exception e) {
                log.error("Fallo al producir episodio {} del lote {}: {}", i + 1, batchJobId, e.getMessage(), e);
                batchJob.setFailedCount(batchJob.getFailedCount() + 1);
            }

            batchJob = batchJobRepository.save(batchJob);
        }

        // Estado final del lote
        BatchJob finalJob = batchJobRepository.findById(batchJobId).orElse(batchJob);
        if (finalJob.getStatus() != BatchJobStatus.CANCELLED) {
            if (finalJob.getCompletedCount() == finalJob.getRequestedCount()) {
                finalJob.setStatus(BatchJobStatus.COMPLETED);
            } else if (finalJob.getCompletedCount() > 0) {
                finalJob.setStatus(BatchJobStatus.COMPLETED);
            } else {
                finalJob.setStatus(BatchJobStatus.FAILED);
            }
            batchJobRepository.save(finalJob);
        }

        log.info("Lote {} finalizado. Estado: {}, Completados: {}/{}, Fallidos: {}",
                batchJobId, finalJob.getStatus(), finalJob.getCompletedCount(), finalJob.getRequestedCount(), finalJob.getFailedCount());
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatch(UUID batchJobId) {
        BatchJob job = batchJobRepository.findById(batchJobId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + batchJobId));
        return BatchResponse.fromEntity(job);
    }

    @Transactional
    public BatchResponse cancelBatch(UUID batchJobId) {
        BatchJob job = batchJobRepository.findById(batchJobId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + batchJobId));

        if (job.getStatus() == BatchJobStatus.IN_PROGRESS || job.getStatus() == BatchJobStatus.PENDING) {
            job.setStatus(BatchJobStatus.CANCELLED);
            job = batchJobRepository.save(job);
            log.info("Lote {} cancelado exitosamente.", batchJobId);
        }

        return BatchResponse.fromEntity(job);
    }
}
