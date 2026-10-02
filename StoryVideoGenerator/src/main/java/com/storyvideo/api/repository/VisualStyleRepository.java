package com.storyvideo.api.repository;

import com.storyvideo.api.domain.VisualStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisualStyleRepository extends JpaRepository<VisualStyle, UUID> {
    Optional<VisualStyle> findByName(String name);
    Optional<VisualStyle> findByIsDefaultTrue();
}
