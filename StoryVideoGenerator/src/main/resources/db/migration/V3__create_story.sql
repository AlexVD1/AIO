CREATE TABLE story (
    id UUID PRIMARY KEY,
    batch_job_id UUID REFERENCES batch_job(id) ON DELETE SET NULL,
    visual_style_id UUID REFERENCES visual_style(id) ON DELETE SET NULL,
    title VARCHAR(500) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    subgenre VARCHAR(255),
    tone VARCHAR(50) NOT NULL,
    theme TEXT,
    hook TEXT NOT NULL,
    premise TEXT NOT NULL,
    synopsis TEXT NOT NULL,
    narrative_arc VARCHAR(50) NOT NULL DEFAULT 'LINEAR',
    twist TEXT,
    ending TEXT NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'es-MX',
    target_duration_seconds INT NOT NULL DEFAULT 90,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_story_genre ON story(genre);
CREATE INDEX idx_story_status ON story(status);
CREATE INDEX idx_story_created_at ON story(created_at);
CREATE INDEX idx_story_batch_job ON story(batch_job_id);
