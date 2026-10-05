package com.kidsanim.api.repository;

import com.kidsanim.api.domain.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SceneRepository extends JpaRepository<Scene, UUID> {
    List<Scene> findByEpisodeIdOrderByOrderIndexAsc(UUID episodeId);
}
