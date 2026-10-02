package com.storyvideo.api.repository;

import com.storyvideo.api.domain.VideoRender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRenderRepository extends JpaRepository<VideoRender, UUID> {
    List<VideoRender> findByVideoProjectIdOrderByAttemptNumberAsc(UUID videoProjectId);
}
