INSERT INTO visual_style (
    id, name, art_style, negative_prompt, quality_modifiers, genre_modifiers_json, is_default
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Cinematic Dark (Default)',
    'cinematic dark photography, film still, 35mm photograph, moody volumetric lighting, deep shadows, atmospheric depth',
    'cartoon, anime, 3d render, CGI, oversaturated, bright colors, text, watermark, bad anatomy, blurry, low quality',
    '8k uhd, masterpiece, highly detailed, photorealistic, sharp focus',
    '{"HORROR":"creepy, unsettling, ominous, decrepit","MYSTERY":"foggy, noir, dim lighting, mysterious silhouette","SCI_FI":"dystopian, holographic glow, neon accents in darkness","PSYCHOLOGICAL":"surreal lighting, claustrophobic, desaturated"}',
    TRUE
), (
    'a0000000-0000-0000-0000-000000000002',
    'Eerie Vintage Found Footage',
    'found footage, VHS glitch effect, 1990s videotape aesthetic, grainy security camera footage, analog distortion',
    'modern photography, high fidelity, 4k, CGI, anime, cartoon, text, colorful',
    'authentic vhs grain, color bleed, authentic timestamp, low contrast',
    '{"URBAN_LEGEND":"amateur camera, flash lighting in dark, liminal space","HORROR":"unsettling tape distortion, night vision green tint"}',
    FALSE
), (
    'a0000000-0000-0000-0000-000000000003',
    'Sci-Fi Cyberpunk Noir',
    'cyberpunk noir, blade runner aesthetic, rain-slicked dark streets, neon reflections in puddles, cinematic anamorphic lens flare',
    'daylight, sunny, cartoon, bright happy, fantasy, text, watermark',
    '8k, Octane render style, ultra detailed, ray tracing reflection',
    '{"DYSTOPIA":"oppressive architecture, smog, surveillance drones","SCI_FI":"cybernetic details, retrofuturistic tech"}',
    FALSE
);
