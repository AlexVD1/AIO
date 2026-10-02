package com.storyvideo.api.repository;

import com.storyvideo.api.domain.StoryCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryCharacterRepository extends JpaRepository<StoryCharacter, UUID> {
    List<StoryCharacter> findByStoryId(UUID storyId);
}
