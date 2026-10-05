-- Fase 11 / Q1.4 (hallazgos V5, V6): generar video en 16:9 nativo y a 24 fps
-- para eliminar las barras negras laterales y la duplicación de frames 16→30 fps.
ALTER TABLE style_profile ALTER COLUMN video_fps SET DEFAULT 24;
ALTER TABLE style_profile ALTER COLUMN video_resolution SET DEFAULT '1024x576';

UPDATE style_profile
SET video_fps = 24,
    video_resolution = '1024x576'
WHERE video_resolution = '768x512' OR video_fps = 16;

-- Q1.7 (hallazgo A2): no ralentizar la síntesis de voz; se usan pausas en su lugar.
UPDATE "character" SET tts_rate = '+0%' WHERE tts_rate = '-12%';
ALTER TABLE "character" ALTER COLUMN tts_rate SET DEFAULT '+0%';
