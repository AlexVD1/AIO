package com.kidsanim.api.repository;

import com.kidsanim.api.domain.BatchJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {
    List<BatchJob> findBySeriesId(UUID seriesId);
}
