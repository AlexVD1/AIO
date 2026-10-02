package com.storyvideo.api.repository;

import com.storyvideo.api.domain.VideoProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoProjectRepository extends JpaRepository<VideoProject, UUID> {
    Optional<VideoProject> findByStoryId(UUID storyId);
}
