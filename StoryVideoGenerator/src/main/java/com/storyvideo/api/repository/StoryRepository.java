package com.storyvideo.api.repository;

import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {

    Page<Story> findByStatus(StoryStatus status, Pageable pageable);

    Page<Story> findByGenre(StoryGenre genre, Pageable pageable);

    Page<Story> findByGenreAndStatus(StoryGenre genre, StoryStatus status, Pageable pageable);

    List<Story> findByBatchJobIdOrderByCreatedAtAsc(UUID batchJobId);

    @Query("SELECT s.premise FROM Story s WHERE s.genre = :genre ORDER BY s.createdAt DESC LIMIT :limit")
    List<String> findRecentPremisesByGenre(@Param("genre") StoryGenre genre, @Param("limit") int limit);
}
