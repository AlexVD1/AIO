package com.kidsanim.api.repository;

import com.kidsanim.api.domain.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, UUID> {
    List<Episode> findBySeriesId(UUID seriesId);
    List<Episode> findByBatchJobId(UUID batchJobId);
    Optional<Episode> findByFingerprint(String fingerprint);
}
