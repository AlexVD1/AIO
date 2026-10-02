package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "story_scene",
        uniqueConstraints = @UniqueConstraint(name = "uq_scene_story_sequence", columnNames = {"story_id", "sequence_number"}),
        indexes = {
                @Index(name = "idx_scene_story_id", columnList = "story_id"),
                @Index(name = "idx_scene_status", columnList = "status")
        })
public class StoryScene {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene_type", nullable = false, length = 50)
    private SceneType sceneType = SceneType.DEVELOPMENT;

    @Column(name = "narration_text", columnDefinition = "TEXT", nullable = false)
    private String narrationText;

    @Column(name = "narration_script", columnDefinition = "TEXT")
    private String narrationScript;

    @Column(name = "narration_audio_path", length = 500)
    private String narrationAudioPath;

    @Column(name = "narration_duration_seconds", precision = 8, scale = 2)
    private BigDecimal narrationDurationSeconds = BigDecimal.ZERO;

    @Column(name = "narration_emotion", length = 255)
    private String narrationEmotion = "NEUTRAL";

    @Column(name = "visual_description", columnDefinition = "TEXT", nullable = false)
    private String visualDescription;

    @Column(name = "visual_prompt", columnDefinition = "TEXT", nullable = false)
    private String visualPrompt;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "image_version", nullable = false)
    private int imageVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_movement", length = 50)
    private CameraMovement cameraMovement = CameraMovement.KEN_BURNS;

    @Column(name = "music_intensity", length = 255)
    private String musicIntensity = "MEDIUM";

    @Column(name = "ambient_sound", length = 255)
    private String ambientSound;

    @Enumerated(EnumType.STRING)
    @Column(name = "transition_in", length = 50)
    private TransitionType transitionIn = TransitionType.FADE_IN;

    @Enumerated(EnumType.STRING)
    @Column(name = "transition_out", length = 50)
    private TransitionType transitionOut = TransitionType.CUT;

    @Column(name = "subtitle_style", length = 255)
    private String subtitleStyle = "STANDARD";

    @Column(name = "estimated_duration_seconds", precision = 8, scale = 2)
    private BigDecimal estimatedDurationSeconds = BigDecimal.ZERO;

    @Column(name = "actual_duration_seconds", precision = 8, scale = 2)
    private BigDecimal actualDurationSeconds = BigDecimal.ZERO;

    @Column(name = "start_time_seconds", precision = 8, scale = 2)
    private BigDecimal startTimeSeconds = BigDecimal.ZERO;

    @Column(name = "end_time_seconds", precision = 8, scale = 2)
    private BigDecimal endTimeSeconds = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SceneStatus status = SceneStatus.PLANNED;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StoryScene() {}

    public StoryScene(int sequenceNumber, SceneType sceneType, String narrationText,
                      String visualDescription, String visualPrompt,
                      CameraMovement cameraMovement) {
        this.sequenceNumber = sequenceNumber;
        this.sceneType = (sceneType != null) ? sceneType : SceneType.DEVELOPMENT;
        this.narrationText = narrationText;
        this.visualDescription = visualDescription;
        this.visualPrompt = visualPrompt;
        this.cameraMovement = (cameraMovement != null) ? cameraMovement : CameraMovement.KEN_BURNS;
        this.status = SceneStatus.PLANNED;
        this.version = 1;
        this.imageVersion = 1;
    }

    public StoryScene(int sequenceNumber, String narrationText) {
        this(sequenceNumber, SceneType.DEVELOPMENT, narrationText, null, null, CameraMovement.SLOW_ZOOM_IN);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public SceneType getSceneType() { return sceneType; }
    public void setSceneType(SceneType sceneType) { this.sceneType = sceneType; }
    public String getNarrationText() { return narrationText; }
    public void setNarrationText(String narrationText) { this.narrationText = narrationText; }
    public String getNarrationScript() { return narrationScript; }
    public void setNarrationScript(String narrationScript) { this.narrationScript = narrationScript; }
    public String getNarrationAudioPath() { return narrationAudioPath; }
    public void setNarrationAudioPath(String narrationAudioPath) { this.narrationAudioPath = narrationAudioPath; }
    public BigDecimal getNarrationDurationSeconds() { return narrationDurationSeconds; }
    public void setNarrationDurationSeconds(BigDecimal narrationDurationSeconds) { this.narrationDurationSeconds = narrationDurationSeconds; }
    public String getNarrationEmotion() { return narrationEmotion; }
    public void setNarrationEmotion(String narrationEmotion) { this.narrationEmotion = narrationEmotion; }
    public String getVisualDescription() { return visualDescription; }
    public void setVisualDescription(String visualDescription) { this.visualDescription = visualDescription; }
    public String getVisualPrompt() { return visualPrompt; }
    public void setVisualPrompt(String visualPrompt) { this.visualPrompt = visualPrompt; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public int getImageVersion() { return imageVersion; }
    public void setImageVersion(int imageVersion) { this.imageVersion = imageVersion; }
    public CameraMovement getCameraMovement() { return cameraMovement; }
    public void setCameraMovement(CameraMovement cameraMovement) { this.cameraMovement = cameraMovement; }
    public String getMusicIntensity() { return musicIntensity; }
    public void setMusicIntensity(String musicIntensity) { this.musicIntensity = musicIntensity; }
    public String getAmbientSound() { return ambientSound; }
    public void setAmbientSound(String ambientSound) { this.ambientSound = ambientSound; }
    public TransitionType getTransitionIn() { return transitionIn; }
    public void setTransitionIn(TransitionType transitionIn) { this.transitionIn = transitionIn; }
    public TransitionType getTransitionOut() { return transitionOut; }
    public void setTransitionOut(TransitionType transitionOut) { this.transitionOut = transitionOut; }
    public String getSubtitleStyle() { return subtitleStyle; }
    public void setSubtitleStyle(String subtitleStyle) { this.subtitleStyle = subtitleStyle; }
    public BigDecimal getEstimatedDurationSeconds() { return estimatedDurationSeconds; }
    public void setEstimatedDurationSeconds(BigDecimal estimatedDurationSeconds) { this.estimatedDurationSeconds = estimatedDurationSeconds; }
    public BigDecimal getActualDurationSeconds() { return actualDurationSeconds; }
    public void setActualDurationSeconds(BigDecimal actualDurationSeconds) { this.actualDurationSeconds = actualDurationSeconds; }
    public BigDecimal getStartTimeSeconds() { return startTimeSeconds; }
    public void setStartTimeSeconds(BigDecimal startTimeSeconds) { this.startTimeSeconds = startTimeSeconds; }
    public BigDecimal getEndTimeSeconds() { return endTimeSeconds; }
    public void setEndTimeSeconds(BigDecimal endTimeSeconds) { this.endTimeSeconds = endTimeSeconds; }
    public SceneStatus getStatus() { return status; }
    public void setStatus(SceneStatus status) { this.status = status; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
