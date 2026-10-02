CREATE TABLE story_fingerprint (
    id UUID PRIMARY KEY,
    story_id UUID NOT NULL UNIQUE REFERENCES story(id) ON DELETE CASCADE,
    title_normalized VARCHAR(500) NOT NULL,
    title_hash VARCHAR(64) NOT NULL UNIQUE,
    premise_hash VARCHAR(64) NOT NULL,
    premise_embedding TEXT,
    twist_hash VARCHAR(64),
    twist_embedding TEXT,
    character_names_json TEXT,
    genre VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fingerprint_genre ON story_fingerprint(genre);
CREATE INDEX idx_fingerprint_premise ON story_fingerprint(premise_hash);
