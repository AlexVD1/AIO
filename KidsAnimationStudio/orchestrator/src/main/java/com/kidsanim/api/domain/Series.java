package com.kidsanim.api.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "series")
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 20)
    private String language = "es-MX";

    @Column(name = "target_age_min", nullable = false)
    private int targetAgeMin = 2;

    @Column(name = "target_age_max", nullable = false)
    private int targetAgeMax = 6;

    @Column(name = "aspect_ratio", nullable = false, length = 10)
    private String aspectRatio = "16:9";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_profile_id", nullable = false)
    private StyleProfile styleProfile;

    @Column(name = "default_bgm_mood", nullable = false, length = 50)
    private String defaultBgmMood = "playful";

    @Column(name = "animation_mode", nullable = false, length = 50)
    private String animationMode = "PUPPET_2D";

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Character> characters = new ArrayList<>();

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Location> locations = new ArrayList<>();

    @OneToMany(mappedBy = "series")
    private List<Episode> episodes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Series() {
    }

    public Series(String name, String description, StyleProfile styleProfile) {
        this.name = name;
        this.description = description;
        this.styleProfile = styleProfile;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getTargetAgeMin() {
        return targetAgeMin;
    }

    public void setTargetAgeMin(int targetAgeMin) {
        this.targetAgeMin = targetAgeMin;
    }

    public int getTargetAgeMax() {
        return targetAgeMax;
    }

    public void setTargetAgeMax(int targetAgeMax) {
        this.targetAgeMax = targetAgeMax;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public StyleProfile getStyleProfile() {
        return styleProfile;
    }

    public void setStyleProfile(StyleProfile styleProfile) {
        this.styleProfile = styleProfile;
    }

    public String getDefaultBgmMood() {
        return defaultBgmMood;
    }

    public void setDefaultBgmMood(String defaultBgmMood) {
        this.defaultBgmMood = defaultBgmMood;
    }

    public List<Character> getCharacters() {
        return characters;
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
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

    public String getAnimationMode() {
        return animationMode;
    }

    public void setAnimationMode(String animationMode) {
        this.animationMode = animationMode;
    }
}
