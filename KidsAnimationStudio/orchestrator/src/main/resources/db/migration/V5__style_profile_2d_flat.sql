-- Migración Fase 11 / Q3.2: StyleProfile "2D Preescolar Plano"
-- Diseñado específicamente para animación preescolar estable (tipo Pocoyó / Bluey / Peppa Pig):
-- Contornos limpios, colores planos vibrantes, sin texturas hiperrealistas ni dedos complejos.

INSERT INTO style_profile (
    id,
    name,
    checkpoint_name,
    style_prompt,
    negative_prompt,
    sampler,
    steps,
    cfg,
    width,
    height,
    video_model,
    video_fps,
    video_resolution
) VALUES (
    '00000000-0000-0000-0000-000000000020',
    '2D Preescolar Plano',
    'DreamShaperXL_Turbo_V2-SFW.safetensors',
    'flat 2D vector animation style, bold smooth outlines, vibrant solid flat colors, simple minimalist shapes, cute preschool children cartoon, storybook illustration, clean background, cheerful warm lighting',
    'photorealistic, realistic fur, human hands with detailed fingers, creepy, scary, dark, horror, 3d render, clay, uncanny, grainy, textured shading, complex gradients, watermark, signature',
    'dpmpp_sde',
    8,
    2.00,
    1024,
    576,
    'LTX',
    24,
    '1024x576'
) ON CONFLICT (id) DO NOTHING;
