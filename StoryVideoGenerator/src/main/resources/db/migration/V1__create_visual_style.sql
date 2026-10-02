CREATE TABLE visual_style (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    art_style TEXT NOT NULL,
    negative_prompt TEXT NOT NULL,
    quality_modifiers TEXT NOT NULL,
    genre_modifiers_json TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visual_style_name ON visual_style(name);
