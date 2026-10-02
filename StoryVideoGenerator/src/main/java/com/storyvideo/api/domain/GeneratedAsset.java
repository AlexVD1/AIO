package com.storyvideo.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_asset", indexes = {
        @Index(name = "idx_asset_scene_id", columnList = "scene_id"),
        @Index(name = "idx_asset_type", columnList = "asset_type")
})
public class GeneratedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id")
    private StoryScene scene;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private AssetType assetType;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes = 0;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AssetStatus status = AssetStatus.READY;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GeneratedAsset() {}

    public GeneratedAsset(StoryScene scene, AssetType assetType, String filePath, String mimeType, long fileSizeBytes, int version) {
        this.scene = scene;
        this.assetType = assetType;
        this.filePath = filePath;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.version = version;
        this.status = AssetStatus.READY;
    }

    public UUID getId() { return id; }
    public StoryScene getScene() { return scene; }
    public void setScene(StoryScene scene) { this.scene = scene; }
    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
