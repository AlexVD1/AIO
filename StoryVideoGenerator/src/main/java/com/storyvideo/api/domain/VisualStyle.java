package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visual_style", indexes = {
        @Index(name = "idx_visual_style_name", columnList = "name")
})
public class VisualStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "art_style", columnDefinition = "TEXT", nullable = false)
    private String artStyle;

    @Column(name = "negative_prompt", columnDefinition = "TEXT", nullable = false)
    private String negativePrompt;

    @Column(name = "quality_modifiers", columnDefinition = "TEXT", nullable = false)
    private String qualityModifiers;

    @Column(name = "genre_modifiers_json", columnDefinition = "TEXT")
    private String genreModifiersJson;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected VisualStyle() {}

    public VisualStyle(String name, String artStyle, String negativePrompt, String qualityModifiers, String genreModifiersJson, boolean isDefault) {
        this.name = name;
        this.artStyle = artStyle;
        this.negativePrompt = negativePrompt;
        this.qualityModifiers = qualityModifiers;
        this.genreModifiersJson = genreModifiersJson;
        this.isDefault = isDefault;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArtStyle() { return artStyle; }
    public void setArtStyle(String artStyle) { this.artStyle = artStyle; }
    public String getNegativePrompt() { return negativePrompt; }
    public void setNegativePrompt(String negativePrompt) { this.negativePrompt = negativePrompt; }
    public String getQualityModifiers() { return qualityModifiers; }
    public void setQualityModifiers(String qualityModifiers) { this.qualityModifiers = qualityModifiers; }
    public String getGenreModifiersJson() { return genreModifiersJson; }
    public void setGenreModifiersJson(String genreModifiersJson) { this.genreModifiersJson = genreModifiersJson; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
