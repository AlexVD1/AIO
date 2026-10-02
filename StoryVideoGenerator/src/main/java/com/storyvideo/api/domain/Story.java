package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "story", indexes = {
        @Index(name = "idx_story_genre", columnList = "genre"),
        @Index(name = "idx_story_status", columnList = "status"),
        @Index(name = "idx_story_created_at", columnList = "created_at"),
        @Index(name = "idx_story_batch_job", columnList = "batch_job_id")
})
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_job_id")
    private BatchJob batchJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visual_style_id")
    private VisualStyle visualStyle;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 50)
    private StoryGenre genre;

    @Column(name = "subgenre", length = 255)
    private String subgenre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", nullable = false, length = 50)
    private StoryTone tone;

    @Column(name = "theme", columnDefinition = "TEXT")
    private String theme;

    @Column(name = "hook", columnDefinition = "TEXT", nullable = false)
    private String hook;

    @Column(name = "premise", columnDefinition = "TEXT", nullable = false)
    private String premise;

    @Column(name = "synopsis", columnDefinition = "TEXT", nullable = false)
    private String synopsis;

    @Enumerated(EnumType.STRING)
    @Column(name = "narrative_arc", nullable = false, length = 50)
    private NarrativeArc narrativeArc = NarrativeArc.LINEAR;

    @Column(name = "twist", columnDefinition = "TEXT")
    private String twist;

    @Column(name = "ending", columnDefinition = "TEXT", nullable = false)
    private String ending;

    @Column(name = "language", nullable = false, length = 10)
    private String language = "es-MX";

    @Column(name = "target_duration_seconds", nullable = false)
    private int targetDurationSeconds = 90;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StoryStatus status = StoryStatus.CREATED;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<StoryScene> scenes = new ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryCharacter> characters = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Story() {}

    public Story(String title, StoryGenre genre, String subgenre, StoryTone tone, String theme,
                 String hook, String premise, String synopsis, NarrativeArc narrativeArc,
                 String twist, String ending, String language, int targetDurationSeconds,
                 VisualStyle visualStyle) {
        this.title = title;
        this.genre = genre;
        this.subgenre = subgenre;
        this.tone = tone;
        this.theme = theme;
        this.hook = hook;
        this.premise = premise;
        this.synopsis = synopsis;
        this.narrativeArc = (narrativeArc != null) ? narrativeArc : NarrativeArc.LINEAR;
        this.twist = twist;
        this.ending = ending;
        this.language = (language != null && !language.isBlank()) ? language : "es-MX";
        this.targetDurationSeconds = targetDurationSeconds;
        this.visualStyle = visualStyle;
        this.status = StoryStatus.CREATED;
        this.version = 1;
    }

    public Story(String title, StoryGenre genre, String theme) {
        this(title, genre, null, StoryTone.SUSPENSE, theme, "Hook inicial obligatorio", "Premisa de prueba", "Sinopsis de prueba", NarrativeArc.LINEAR, null, "Desenlace", "es-MX", 60, null);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BatchJob getBatchJob() { return batchJob; }
    public void setBatchJob(BatchJob batchJob) { this.batchJob = batchJob; }
    public VisualStyle getVisualStyle() { return visualStyle; }
    public void setVisualStyle(VisualStyle visualStyle) { this.visualStyle = visualStyle; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public StoryGenre getGenre() { return genre; }
    public void setGenre(StoryGenre genre) { this.genre = genre; }
    public String getSubgenre() { return subgenre; }
    public void setSubgenre(String subgenre) { this.subgenre = subgenre; }
    public StoryTone getTone() { return tone; }
    public void setTone(StoryTone tone) { this.tone = tone; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getHook() { return hook; }
    public void setHook(String hook) { this.hook = hook; }
    public String getPremise() { return premise; }
    public void setPremise(String premise) { this.premise = premise; }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public NarrativeArc getNarrativeArc() { return narrativeArc; }
    public void setNarrativeArc(NarrativeArc narrativeArc) { this.narrativeArc = narrativeArc; }
    public String getTwist() { return twist; }
    public void setTwist(String twist) { this.twist = twist; }
    public String getEnding() { return ending; }
    public void setEnding(String ending) { this.ending = ending; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public int getTargetDurationSeconds() { return targetDurationSeconds; }
    public void setTargetDurationSeconds(int targetDurationSeconds) { this.targetDurationSeconds = targetDurationSeconds; }
    public StoryStatus getStatus() { return status; }
    public void setStatus(StoryStatus status) { this.status = status; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public List<StoryScene> getScenes() { return scenes; }
    public void setScenes(List<StoryScene> scenes) { this.scenes = scenes; }
    public List<StoryCharacter> getCharacters() { return characters; }
    public void setCharacters(List<StoryCharacter> characters) { this.characters = characters; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void addScene(StoryScene scene) {
        scenes.add(scene);
        scene.setStory(this);
    }

    public void addCharacter(StoryCharacter character) {
        characters.add(character);
        character.setStory(this);
    }
}
