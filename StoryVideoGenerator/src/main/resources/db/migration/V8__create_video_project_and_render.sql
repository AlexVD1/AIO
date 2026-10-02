CREATE TABLE video_project (
    id UUID PRIMARY KEY,
    story_id UUID NOT NULL UNIQUE REFERENCES story(id) ON DELETE CASCADE,
    timeline_json TEXT,
    editing_plan_json TEXT,
    audio_mix_json TEXT,
    total_duration_seconds NUMERIC(8,2) DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE video_render (
    id UUID PRIMARY KEY,
    video_project_id UUID NOT NULL REFERENCES video_project(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL DEFAULT 1,
    video_path VARCHAR(500),
    resolution VARCHAR(50) DEFAULT '1080x1920',
    codec VARCHAR(50) DEFAULT 'h264',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    validation_result_json TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_video_project_story_id ON video_project(story_id);
CREATE INDEX idx_video_render_project_id ON video_render(video_project_id);
