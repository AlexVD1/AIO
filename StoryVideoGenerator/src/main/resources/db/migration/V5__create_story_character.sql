CREATE TABLE story_character (
    id UUID PRIMARY KEY,
    story_id UUID NOT NULL REFERENCES story(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    physical_description TEXT NOT NULL,
    distinctive_features TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'PROTAGONIST',
    prompt_fragment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_character_story_id ON story_character(story_id);
