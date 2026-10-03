package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "story_character", indexes = {
        @Index(name = "idx_character_story_id", columnList = "story_id")
})
public class StoryCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "physical_description", columnDefinition = "TEXT", nullable = false)
    private String physicalDescription;

    @Column(name = "distinctive_features", columnDefinition = "TEXT")
    private String distinctiveFeatures;

    @Column(name = "role", nullable = false, length = 255)
    private String role = "PROTAGONIST";

    @Column(name = "prompt_fragment", columnDefinition = "TEXT")
    private String promptFragment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StoryCharacter() {}

    public StoryCharacter(String name, String physicalDescription, String distinctiveFeatures, String role, String promptFragment) {
        this.name = (name != null && name.length() > 250) ? name.substring(0, 250) : (name != null ? name : "Personaje");
        this.physicalDescription = physicalDescription != null ? physicalDescription : "";
        this.distinctiveFeatures = distinctiveFeatures;
        if (role != null && !role.isBlank()) {
            this.role = role.length() > 250 ? role.substring(0, 250) : role;
        } else {
            this.role = "PROTAGONIST";
        }
        this.promptFragment = promptFragment;
    }

    public UUID getId() { return id; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhysicalDescription() { return physicalDescription; }
    public void setPhysicalDescription(String physicalDescription) { this.physicalDescription = physicalDescription; }
    public String getDistinctiveFeatures() { return distinctiveFeatures; }
    public void setDistinctiveFeatures(String distinctiveFeatures) { this.distinctiveFeatures = distinctiveFeatures; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPromptFragment() { return promptFragment; }
    public void setPromptFragment(String promptFragment) { this.promptFragment = promptFragment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
