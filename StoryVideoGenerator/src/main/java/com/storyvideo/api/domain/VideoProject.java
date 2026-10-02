package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "video_project", indexes = {
        @Index(name = "idx_video_project_story_id", columnList = "story_id")
})
public class VideoProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false, unique = true)
    private Story story;

    @Column(name = "timeline_json", columnDefinition = "TEXT")
    private String timelineJson;

    @Column(name = "editing_plan_json", columnDefinition = "TEXT")
    private String editingPlanJson;

    @Column(name = "audio_mix_json", columnDefinition = "TEXT")
    private String audioMixJson;

    @Column(name = "total_duration_seconds", precision = 8, scale = 2)
    private BigDecimal totalDurationSeconds = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "CREATED";

    @OneToMany(mappedBy = "videoProject", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptNumber ASC")
    private List<VideoRender> renders = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected VideoProject() {}

    public VideoProject(Story story) {
        this.story = story;
        this.status = "CREATED";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public String getTimelineJson() { return timelineJson; }
    public void setTimelineJson(String timelineJson) { this.timelineJson = timelineJson; }
    public String getEditingPlanJson() { return editingPlanJson; }
    public void setEditingPlanJson(String editingPlanJson) { this.editingPlanJson = editingPlanJson; }
    public String getAudioMixJson() { return audioMixJson; }
    public void setAudioMixJson(String audioMixJson) { this.audioMixJson = audioMixJson; }
    public BigDecimal getTotalDurationSeconds() { return totalDurationSeconds; }
    public void setTotalDurationSeconds(BigDecimal totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<VideoRender> getRenders() { return renders; }
    public void setRenders(List<VideoRender> renders) { this.renders = renders; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void addRender(VideoRender render) {
        renders.add(render);
        render.setVideoProject(this);
    }
}
