-- Fase 11 / Q2.3: Soporte de abstracción de motor TTS (KOKORO, CLONED, RECORDED)
-- y configuración de voz de referencia por personaje.
ALTER TABLE "character" ADD COLUMN voice_engine VARCHAR(50) NOT NULL DEFAULT 'KOKORO';
ALTER TABLE "character" ADD COLUMN voice_reference_path TEXT;
ALTER TABLE "character" ADD COLUMN voice_expressiveness NUMERIC(3, 2) NOT NULL DEFAULT 0.50;
