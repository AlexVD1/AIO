package com.storyvideo.api.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_render", indexes = {
        @Index(name = "idx_video_render_project_id", columnList = "video_project_id")
})
public class VideoRender {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_project_id", nullable = false)
    private VideoProject videoProject;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "video_path", length = 500)
    private String videoPath;

    @Column(name = "resolution", length = 50)
    private String resolution = "1080x1920";

    @Column(name = "codec", length = 50)
    private String codec = "h264";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VideoRenderStatus status = VideoRenderStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "validation_result_json", columnDefinition = "TEXT")
    private String validationResultJson;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected VideoRender() {}

    public VideoRender(VideoProject videoProject, int attemptNumber) {
        this.videoProject = videoProject;
        this.attemptNumber = attemptNumber;
        this.status = VideoRenderStatus.PENDING;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public VideoProject getVideoProject() { return videoProject; }
    public void setVideoProject(VideoProject videoProject) { this.videoProject = videoProject; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public VideoRenderStatus getStatus() { return status; }
    public void setStatus(VideoRenderStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getValidationResultJson() { return validationResultJson; }
    public void setValidationResultJson(String validationResultJson) { this.validationResultJson = validationResultJson; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
