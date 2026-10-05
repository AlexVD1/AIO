package com.kidsanim.api.domain;

import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.CharacterStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"character\"")
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CharacterRole role = CharacterRole.HOST;

    @Column(name = "canonical_prompt", nullable = false, columnDefinition = "text")
    private String canonicalPrompt;

    @Column(columnDefinition = "text")
    private String personality;

    @Column(name = "tts_voice", nullable = false, length = 50)
    private String ttsVoice = "ef_dora";

    @Column(name = "tts_rate", nullable = false, length = 20)
    private String ttsRate = "+0%";

    @Column(name = "tts_pitch", nullable = false, length = 20)
    private String ttsPitch = "default";

    @Column(name = "voice_engine", nullable = false, length = 50)
    private String voiceEngine = "KOKORO";

    @Column(name = "voice_reference_path", columnDefinition = "text")
    private String voiceReferencePath;

    @Column(name = "voice_expressiveness", nullable = false, precision = 3, scale = 2)
    private BigDecimal voiceExpressiveness = new BigDecimal("0.50");

    @Column(name = "reference_image_path", columnDefinition = "text")
    private String referenceImagePath;

    @Column(name = "reference_seed")
    private Long referenceSeed;

    @Column(name = "ipadapter_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal ipadapterWeight = new BigDecimal("0.85");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CharacterStatus status = CharacterStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Character() {
    }

    public Character(Series series, String name, CharacterRole role, String canonicalPrompt) {
        this.series = series;
        this.name = name;
        this.role = role;
        this.canonicalPrompt = canonicalPrompt;
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

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CharacterRole getRole() {
        return role;
    }

    public void setRole(CharacterRole role) {
        this.role = role;
    }

    public String getCanonicalPrompt() {
        return canonicalPrompt;
    }

    public void setCanonicalPrompt(String canonicalPrompt) {
        this.canonicalPrompt = canonicalPrompt;
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getTtsVoice() {
        return ttsVoice;
    }

    public void setTtsVoice(String ttsVoice) {
        this.ttsVoice = ttsVoice;
    }

    public String getTtsRate() {
        return ttsRate;
    }

    public void setTtsRate(String ttsRate) {
        this.ttsRate = ttsRate;
    }

    public String getTtsPitch() {
        return ttsPitch;
    }

    public void setTtsPitch(String ttsPitch) {
        this.ttsPitch = ttsPitch;
    }

    public String getReferenceImagePath() {
        return referenceImagePath;
    }

    public void setReferenceImagePath(String referenceImagePath) {
        this.referenceImagePath = referenceImagePath;
    }

    public Long getReferenceSeed() {
        return referenceSeed;
    }

    public void setReferenceSeed(Long referenceSeed) {
        this.referenceSeed = referenceSeed;
    }

    public BigDecimal getIpadapterWeight() {
        return ipadapterWeight;
    }

    public void setIpadapterWeight(BigDecimal ipadapterWeight) {
        this.ipadapterWeight = ipadapterWeight;
    }

    public CharacterStatus getStatus() {
        return status;
    }

    public void setStatus(CharacterStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getVoiceEngine() {
        return voiceEngine;
    }

    public void setVoiceEngine(String voiceEngine) {
        this.voiceEngine = voiceEngine;
    }

    public String getVoiceReferencePath() {
        return voiceReferencePath;
    }

    public void setVoiceReferencePath(String voiceReferencePath) {
        this.voiceReferencePath = voiceReferencePath;
    }

    public BigDecimal getVoiceExpressiveness() {
        return voiceExpressiveness;
    }

    public void setVoiceExpressiveness(BigDecimal voiceExpressiveness) {
        this.voiceExpressiveness = voiceExpressiveness;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
