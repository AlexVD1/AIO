package com.storyvideo.api.repository;

import com.storyvideo.api.domain.BatchJob;
import com.storyvideo.api.domain.BatchJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {
    List<BatchJob> findByStatusOrderByCreatedAtDesc(BatchJobStatus status);
}
