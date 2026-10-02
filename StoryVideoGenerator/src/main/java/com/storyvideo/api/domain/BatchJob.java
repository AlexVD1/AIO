package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_job", indexes = {
        @Index(name = "idx_batch_job_status", columnList = "status"),
        @Index(name = "idx_batch_job_created_at", columnList = "created_at")
})
public class BatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "total_requested", nullable = false)
    private int totalRequested;

    @Column(name = "total_completed", nullable = false)
    private int totalCompleted = 0;

    @Column(name = "total_failed", nullable = false)
    private int totalFailed = 0;

    @Column(name = "genre_distribution_json", columnDefinition = "TEXT")
    private String genreDistributionJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BatchJobStatus status = BatchJobStatus.CREATED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected BatchJob() {}

    public BatchJob(int totalRequested, String genreDistributionJson) {
        this.totalRequested = totalRequested;
        this.genreDistributionJson = genreDistributionJson;
        this.status = BatchJobStatus.CREATED;
    }

    public UUID getId() { return id; }
    public int getTotalRequested() { return totalRequested; }
    public void setTotalRequested(int totalRequested) { this.totalRequested = totalRequested; }
    public int getTotalCompleted() { return totalCompleted; }
    public void setTotalCompleted(int totalCompleted) { this.totalCompleted = totalCompleted; }
    public int getTotalFailed() { return totalFailed; }
    public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }
    public String getGenreDistributionJson() { return genreDistributionJson; }
    public void setGenreDistributionJson(String genreDistributionJson) { this.genreDistributionJson = genreDistributionJson; }
    public BatchJobStatus getStatus() { return status; }
    public void setStatus(BatchJobStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
