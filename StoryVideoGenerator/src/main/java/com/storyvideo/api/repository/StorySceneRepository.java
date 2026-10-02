package com.storyvideo.api.repository;

import com.storyvideo.api.domain.StoryScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorySceneRepository extends JpaRepository<StoryScene, UUID> {
    List<StoryScene> findByStoryIdOrderBySequenceNumberAsc(UUID storyId);
    Optional<StoryScene> findByStoryIdAndSequenceNumber(UUID storyId, int sequenceNumber);
}
