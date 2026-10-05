package com.kidsanim.api.domain;

import com.kidsanim.api.domain.enums.VideoModel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "style_profile")
public class StyleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "checkpoint_name", nullable = false)
    private String checkpointName;

    @Column(name = "style_prompt", nullable = false, columnDefinition = "text")
    private String stylePrompt;

    @Column(name = "negative_prompt", nullable = false, columnDefinition = "text")
    private String negativePrompt;

    @Column(nullable = false, length = 50)
    private String sampler = "dpmpp_sde";

    @Column(nullable = false)
    private int steps = 8;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal cfg = new BigDecimal("2.00");

    @Column(nullable = false)
    private int width = 1024;

    @Column(nullable = false)
    private int height = 576;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_model", nullable = false, length = 50)
    private VideoModel videoModel = VideoModel.LTX;

    @Column(name = "video_fps", nullable = false)
    private int videoFps = 24;

    @Column(name = "video_resolution", nullable = false, length = 50)
    private String videoResolution = "1024x576";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public StyleProfile() {
    }

    public StyleProfile(String name, String checkpointName, String stylePrompt, String negativePrompt) {
        this.name = name;
        this.checkpointName = checkpointName;
        this.stylePrompt = stylePrompt;
        this.negativePrompt = negativePrompt;
    }

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCheckpointName() {
        return checkpointName;
    }

    public void setCheckpointName(String checkpointName) {
        this.checkpointName = checkpointName;
    }

    public String getStylePrompt() {
        return stylePrompt;
    }

    public void setStylePrompt(String stylePrompt) {
        this.stylePrompt = stylePrompt;
    }

    public String getNegativePrompt() {
        return negativePrompt;
    }

    public void setNegativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
    }

    public String getSampler() {
        return sampler;
    }

    public void setSampler(String sampler) {
        this.sampler = sampler;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public BigDecimal getCfg() {
        return cfg;
    }

    public void setCfg(BigDecimal cfg) {
        this.cfg = cfg;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public VideoModel getVideoModel() {
        return videoModel;
    }

    public void setVideoModel(VideoModel videoModel) {
        this.videoModel = videoModel;
    }

    public int getVideoFps() {
        return videoFps;
    }

    public void setVideoFps(int videoFps) {
        this.videoFps = videoFps;
    }

    public String getVideoResolution() {
        return videoResolution;
    }

    public void setVideoResolution(String videoResolution) {
        this.videoResolution = videoResolution;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
