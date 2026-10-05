package com.kidsanim.api.repository;

import com.kidsanim.api.domain.PipelineJob;
import com.kidsanim.api.domain.enums.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineJobRepository extends JpaRepository<PipelineJob, UUID> {
    List<PipelineJob> findByEpisodeIdOrderByCreatedAtDesc(UUID episodeId);
    Optional<PipelineJob> findByEpisodeIdAndStage(UUID episodeId, PipelineStage stage);
}
