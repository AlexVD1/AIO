package com.kidsanim.api.repository;

import com.kidsanim.api.domain.Shot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShotRepository extends JpaRepository<Shot, UUID> {
    List<Shot> findBySceneIdOrderByOrderIndexAsc(UUID sceneId);
}
