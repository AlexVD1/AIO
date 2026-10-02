CREATE TABLE generated_asset (
    id UUID PRIMARY KEY,
    scene_id UUID REFERENCES story_scene(id) ON DELETE CASCADE,
    asset_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_asset_scene_id ON generated_asset(scene_id);
CREATE INDEX idx_asset_type ON generated_asset(asset_type);
