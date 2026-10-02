package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "story_fingerprint", indexes = {
        @Index(name = "idx_fingerprint_genre", columnList = "genre"),
        @Index(name = "idx_fingerprint_premise", columnList = "premise_hash")
})
public class StoryFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false, unique = true)
    private Story story;

    @Column(name = "title_normalized", nullable = false, length = 500)
    private String titleNormalized;

    @Column(name = "title_hash", nullable = false, unique = true, length = 64)
    private String titleHash;

    @Column(name = "premise_hash", nullable = false, length = 64)
    private String premiseHash;

    @Column(name = "premise_embedding", columnDefinition = "TEXT")
    private String premiseEmbedding;

    @Column(name = "twist_hash", length = 64)
    private String twistHash;

    @Column(name = "twist_embedding", columnDefinition = "TEXT")
    private String twistEmbedding;

    @Column(name = "character_names_json", columnDefinition = "TEXT")
    private String characterNamesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 50)
    private StoryGenre genre;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StoryFingerprint() {}

    public StoryFingerprint(Story story, String titleNormalized, String titleHash, String premiseHash,
                            String premiseEmbedding, String twistHash, String twistEmbedding,
                            String characterNamesJson, StoryGenre genre) {
        this.story = story;
        this.titleNormalized = titleNormalized;
        this.titleHash = titleHash;
        this.premiseHash = premiseHash;
        this.premiseEmbedding = premiseEmbedding;
        this.twistHash = twistHash;
        this.twistEmbedding = twistEmbedding;
        this.characterNamesJson = characterNamesJson;
        this.genre = genre;
    }

    public UUID getId() { return id; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public String getTitleNormalized() { return titleNormalized; }
    public void setTitleNormalized(String titleNormalized) { this.titleNormalized = titleNormalized; }
    public String getTitleHash() { return titleHash; }
    public void setTitleHash(String titleHash) { this.titleHash = titleHash; }
    public String getPremiseHash() { return premiseHash; }
    public void setPremiseHash(String premiseHash) { this.premiseHash = premiseHash; }
    public String getPremiseEmbedding() { return premiseEmbedding; }
    public void setPremiseEmbedding(String premiseEmbedding) { this.premiseEmbedding = premiseEmbedding; }
    public String getTwistHash() { return twistHash; }
    public void setTwistHash(String twistHash) { this.twistHash = twistHash; }
    public String getTwistEmbedding() { return twistEmbedding; }
    public void setTwistEmbedding(String twistEmbedding) { this.twistEmbedding = twistEmbedding; }
    public String getCharacterNamesJson() { return characterNamesJson; }
    public void setCharacterNamesJson(String characterNamesJson) { this.characterNamesJson = characterNamesJson; }
    public StoryGenre getGenre() { return genre; }
    public void setGenre(StoryGenre genre) { this.genre = genre; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
