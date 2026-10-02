CREATE TABLE story_scene (
    id UUID PRIMARY KEY,
    story_id UUID NOT NULL REFERENCES story(id) ON DELETE CASCADE,
    sequence_number INT NOT NULL,
    scene_type VARCHAR(50) NOT NULL DEFAULT 'DEVELOPMENT',
    narration_text TEXT NOT NULL,
    narration_script TEXT,
    narration_audio_path VARCHAR(500),
    narration_duration_seconds NUMERIC(8,2) DEFAULT 0.00,
    narration_emotion VARCHAR(255) DEFAULT 'NEUTRAL',
    visual_description TEXT NOT NULL,
    visual_prompt TEXT NOT NULL,
    image_path VARCHAR(500),
    image_version INT NOT NULL DEFAULT 1,
    camera_movement VARCHAR(255) DEFAULT 'KEN_BURNS',
    music_intensity VARCHAR(255) DEFAULT 'MEDIUM',
    ambient_sound VARCHAR(255),
    transition_in VARCHAR(255) DEFAULT 'FADE_IN',
    transition_out VARCHAR(255) DEFAULT 'CUT',
    subtitle_style VARCHAR(255) DEFAULT 'STANDARD',
    estimated_duration_seconds NUMERIC(8,2) DEFAULT 0.00,
    actual_duration_seconds NUMERIC(8,2) DEFAULT 0.00,
    start_time_seconds NUMERIC(8,2) DEFAULT 0.00,
    end_time_seconds NUMERIC(8,2) DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_scene_story_sequence UNIQUE (story_id, sequence_number)
);

CREATE INDEX idx_scene_story_id ON story_scene(story_id);
CREATE INDEX idx_scene_status ON story_scene(status);
