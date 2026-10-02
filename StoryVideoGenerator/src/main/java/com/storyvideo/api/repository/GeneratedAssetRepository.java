package com.storyvideo.api.repository;

import com.storyvideo.api.domain.AssetType;
import com.storyvideo.api.domain.GeneratedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratedAssetRepository extends JpaRepository<GeneratedAsset, UUID> {
    List<GeneratedAsset> findBySceneId(UUID sceneId);
    List<GeneratedAsset> findBySceneIdAndAssetType(UUID sceneId, AssetType assetType);
}
