package com.storyvideo.api.repository;

import com.storyvideo.api.domain.StoryFingerprint;
import com.storyvideo.api.domain.StoryGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryFingerprintRepository extends JpaRepository<StoryFingerprint, UUID> {
    boolean existsByTitleHash(String titleHash);
    boolean existsByPremiseHash(String premiseHash);
    Optional<StoryFingerprint> findByStoryId(UUID storyId);
    List<StoryFingerprint> findTop50ByGenreOrderByCreatedAtDesc(StoryGenre genre);
}
