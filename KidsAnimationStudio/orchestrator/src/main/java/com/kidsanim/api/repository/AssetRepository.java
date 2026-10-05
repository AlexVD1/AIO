package com.kidsanim.api.repository;

import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByEpisodeId(UUID episodeId);
    List<Asset> findByShotId(UUID shotId);
    List<Asset> findByEpisodeIdAndType(UUID episodeId, AssetType type);
    java.util.Optional<Asset> findByShotIdAndType(UUID shotId, AssetType type);
}
