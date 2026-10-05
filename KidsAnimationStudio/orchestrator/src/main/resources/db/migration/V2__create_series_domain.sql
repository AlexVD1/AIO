-- Migración Fase 2: Dominio y biblia de la serie (KidsAnimationStudio)
-- Tablas: style_profile, series, character, location, batch_job, episode, scene, shot, asset, pipeline_job

-- 1. style_profile
CREATE TABLE style_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    checkpoint_name VARCHAR(255) NOT NULL,
    style_prompt TEXT NOT NULL,
    negative_prompt TEXT NOT NULL,
    sampler VARCHAR(50) NOT NULL DEFAULT 'dpmpp_sde',
    steps INT NOT NULL DEFAULT 8,
    cfg NUMERIC(4,2) NOT NULL DEFAULT 2.0,
    width INT NOT NULL DEFAULT 1024,
    height INT NOT NULL DEFAULT 576,
    video_model VARCHAR(50) NOT NULL DEFAULT 'LTX',
    video_fps INT NOT NULL DEFAULT 16,
    video_resolution VARCHAR(50) NOT NULL DEFAULT '768x512',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. series
CREATE TABLE series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    language VARCHAR(20) NOT NULL DEFAULT 'es-MX',
    target_age_min INT NOT NULL DEFAULT 2,
    target_age_max INT NOT NULL DEFAULT 6,
    aspect_ratio VARCHAR(10) NOT NULL DEFAULT '16:9',
    style_profile_id UUID NOT NULL REFERENCES style_profile(id) ON DELETE RESTRICT,
    default_bgm_mood VARCHAR(50) NOT NULL DEFAULT 'playful',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_series_style_profile_id ON series(style_profile_id);

-- 3. character
CREATE TABLE "character" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id UUID NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'HOST',
    canonical_prompt TEXT NOT NULL,
    personality TEXT,
    tts_voice VARCHAR(50) NOT NULL DEFAULT 'ef_dora',
    tts_rate VARCHAR(20) NOT NULL DEFAULT '-12%',
    tts_pitch VARCHAR(20) NOT NULL DEFAULT 'default',
    reference_image_path TEXT,
    reference_seed BIGINT,
    ipadapter_weight NUMERIC(3,2) NOT NULL DEFAULT 0.85,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_character_series_id ON "character"(series_id);

-- 4. location
CREATE TABLE location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id UUID NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    prompt TEXT NOT NULL,
    reference_image_path TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_location_series_id ON location(series_id);

-- 5. batch_job
CREATE TABLE batch_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    series_id UUID REFERENCES series(id) ON DELETE SET NULL,
    requested_count INT NOT NULL DEFAULT 1,
    completed_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_batch_job_series_id ON batch_job(series_id);
CREATE INDEX idx_batch_job_status ON batch_job(status);

-- 6. episode
CREATE TABLE episode (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id UUID NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    batch_job_id UUID REFERENCES batch_job(id) ON DELETE SET NULL,
    topic_type VARCHAR(50) NOT NULL,
    topic_detail TEXT NOT NULL,
    learning_objective TEXT NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    script_json JSONB,
    fingerprint VARCHAR(128),
    final_video_path TEXT,
    duration_seconds NUMERIC(6,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_episode_series_id ON episode(series_id);
CREATE INDEX idx_episode_batch_job_id ON episode(batch_job_id);
CREATE INDEX idx_episode_status ON episode(status);
CREATE INDEX idx_episode_fingerprint ON episode(fingerprint);

-- 7. scene
CREATE TABLE scene (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id UUID NOT NULL REFERENCES episode(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    location_id UUID REFERENCES location(id) ON DELETE RESTRICT,
    purpose VARCHAR(50) NOT NULL,
    bgm_mood VARCHAR(50) NOT NULL DEFAULT 'playful',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scene_episode_id ON scene(episode_id);
CREATE INDEX idx_scene_location_id ON scene(location_id);

-- 8. shot
CREATE TABLE shot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scene_id UUID NOT NULL REFERENCES scene(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    character_id UUID REFERENCES "character"(id) ON DELETE SET NULL,
    narration_text TEXT NOT NULL,
    speaker VARCHAR(100) NOT NULL DEFAULT 'NARRATOR',
    visual_prompt TEXT NOT NULL,
    camera_motion VARCHAR(50) NOT NULL DEFAULT 'STATIC',
    action_prompt TEXT,
    continuity_mode VARCHAR(50) NOT NULL DEFAULT 'NEW_KEYFRAME',
    overlays_json JSONB,
    sfx_json JSONB,
    pause_after_ms INT NOT NULL DEFAULT 300,
    target_duration_ms INT NOT NULL DEFAULT 4000,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    similarity_score NUMERIC(4,3),
    attempts INT NOT NULL DEFAULT 0,
    seed BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shot_scene_id ON shot(scene_id);
CREATE INDEX idx_shot_character_id ON shot(character_id);

-- 9. asset
CREATE TABLE asset (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id UUID NOT NULL REFERENCES episode(id) ON DELETE CASCADE,
    shot_id UUID REFERENCES shot(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    path TEXT NOT NULL,
    checksum VARCHAR(64),
    meta_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_asset_episode_id ON asset(episode_id);
CREATE INDEX idx_asset_shot_id ON asset(shot_id);
CREATE INDEX idx_asset_type ON asset(type);

-- 10. pipeline_job
CREATE TABLE pipeline_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id UUID NOT NULL REFERENCES episode(id) ON DELETE CASCADE,
    stage VARCHAR(50) NOT NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    attempt INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pipeline_job_episode_id ON pipeline_job(episode_id);
CREATE INDEX idx_pipeline_job_status ON pipeline_job(status);

-- ============================================================
-- SEED DATA (Fase 2)
-- Serie de ejemplo: "Tito el zorrito" + StyleProfile + 3 Locaciones
-- ============================================================

INSERT INTO style_profile (
    id, name, checkpoint_name, style_prompt, negative_prompt, sampler, steps, cfg, width, height, video_model, video_fps, video_resolution
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    '3D Cartoon Pixar Style',
    'DreamShaperXL_Turbo_V2-SFW.safetensors',
    '3d animation, pixar style, cute cartoon, vibrant colors, soft warm studio lighting, highly detailed, clean render, child-friendly',
    'scary, dark, horror, realistic photo, ugly, deformed, blurry, bad anatomy, weapons, violence, text, watermark, signature',
    'dpmpp_sde',
    8,
    2.00,
    1024,
    576,
    'LTX',
    16,
    '768x512'
);

INSERT INTO series (
    id, name, description, language, target_age_min, target_age_max, aspect_ratio, style_profile_id, default_bgm_mood
) VALUES (
    '00000000-0000-0000-0000-000000000002',
    'Tito el zorrito',
    'Aventuras educativas en el bosque con Tito, un pequeño zorro curioso y sus amigos.',
    'es-MX',
    2,
    5,
    '16:9',
    '00000000-0000-0000-0000-000000000001',
    'playful'
);

INSERT INTO "character" (
    id, series_id, name, role, canonical_prompt, personality, tts_voice, tts_rate, tts_pitch, reference_image_path, reference_seed, ipadapter_weight, status
) VALUES (
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000002',
    'Tito',
    'HOST',
    'a cute little red fox kit named Tito, big warm curious brown eyes, fluffy white-tipped tail, wearing a tiny yellow vest, cheerful friendly expression',
    'Curioso, entusiasta, alegre, le encanta cantar y contar cosas.',
    'ef_dora',
    '-12%',
    'default',
    NULL,
    42,
    0.85,
    'DRAFT'
);

INSERT INTO location (
    id, series_id, name, prompt, reference_image_path
) VALUES (
    '00000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000002',
    'Huerto Soleado',
    'a bright sunny apple orchard, colorful green rolling grass, cute rounded apple trees full of red apples, clear blue sky with fluffy white clouds, warm morning sunlight',
    NULL
), (
    '00000000-0000-0000-0000-000000000012',
    '00000000-0000-0000-0000-000000000002',
    'Claro del Bosque',
    'a magical enchanted forest clearing with giant colorful friendly mushrooms, soft moss, sparkling gentle stream, flowers everywhere, welcoming atmosphere',
    NULL
), (
    '00000000-0000-0000-0000-000000000013',
    '00000000-0000-0000-0000-000000000002',
    'Cabaña de Tito',
    'cozy rustic wooden treehouse bedroom, round window overlooking the forest, tiny wooden desk with colorful toy blocks, warm lantern light, happy nursery vibe',
    NULL
);
