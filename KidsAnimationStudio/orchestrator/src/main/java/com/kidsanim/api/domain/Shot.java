package com.kidsanim.api.domain;

import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.ShotStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shot")
public class Shot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    @Column(name = "narration_text", nullable = false, columnDefinition = "text")
    private String narrationText;

    @Column(nullable = false, length = 100)
    private String speaker = "NARRATOR";

    @Column(name = "visual_prompt", nullable = false, columnDefinition = "text")
    private String visualPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_motion", nullable = false, length = 50)
    private CameraMotion cameraMotion = CameraMotion.STATIC;

    @Column(name = "action_prompt", columnDefinition = "text")
    private String actionPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "continuity_mode", nullable = false, length = 50)
    private ContinuityMode continuityMode = ContinuityMode.NEW_KEYFRAME;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "overlays_json", columnDefinition = "jsonb")
    private String overlaysJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sfx_json", columnDefinition = "jsonb")
    private String sfxJson;

    @Column(name = "pause_after_ms", nullable = false)
    private int pauseAfterMs = 300;

    @Column(name = "target_duration_ms", nullable = false)
    private int targetDurationMs = 4000;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ShotStatus status = ShotStatus.PENDING;

    @Column(name = "similarity_score", precision = 4, scale = 3)
    private BigDecimal similarityScore;

    @Column(nullable = false)
    private int attempts = 0;

    private Long seed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Shot() {
    }

    public Shot(Scene scene, int orderIndex, String narrationText, String visualPrompt) {
        this.scene = scene;
        this.orderIndex = orderIndex;
        this.narrationText = narrationText;
        this.visualPrompt = visualPrompt;
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

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public String getNarrationText() {
        return narrationText;
    }

    public void setNarrationText(String narrationText) {
        this.narrationText = narrationText;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getVisualPrompt() {
        return visualPrompt;
    }

    public void setVisualPrompt(String visualPrompt) {
        this.visualPrompt = visualPrompt;
    }

    public CameraMotion getCameraMotion() {
        return cameraMotion;
    }

    public void setCameraMotion(CameraMotion cameraMotion) {
        this.cameraMotion = cameraMotion;
    }

    public String getActionPrompt() {
        return actionPrompt;
    }

    public void setActionPrompt(String actionPrompt) {
        this.actionPrompt = actionPrompt;
    }

    public ContinuityMode getContinuityMode() {
        return continuityMode;
    }

    public void setContinuityMode(ContinuityMode continuityMode) {
        this.continuityMode = continuityMode;
    }

    public String getOverlaysJson() {
        return overlaysJson;
    }

    public void setOverlaysJson(String overlaysJson) {
        this.overlaysJson = overlaysJson;
    }

    public String getSfxJson() {
        return sfxJson;
    }

    public void setSfxJson(String sfxJson) {
        this.sfxJson = sfxJson;
    }

    public int getPauseAfterMs() {
        return pauseAfterMs;
    }

    public void setPauseAfterMs(int pauseAfterMs) {
        this.pauseAfterMs = pauseAfterMs;
    }

    public int getTargetDurationMs() {
        return targetDurationMs;
    }

    public void setTargetDurationMs(int targetDurationMs) {
        this.targetDurationMs = targetDurationMs;
    }

    public ShotStatus getStatus() {
        return status;
    }

    public void setStatus(ShotStatus status) {
        this.status = status;
    }

    public BigDecimal getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(BigDecimal similarityScore) {
        this.similarityScore = similarityScore;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
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
