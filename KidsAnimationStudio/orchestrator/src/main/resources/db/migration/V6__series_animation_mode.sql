-- Migración Fase 11 / Q4.7: Modo de animación por serie
-- Opciones: 'PUPPET_2D' (por defecto preescolar determinista), 'GENERATIVE_I2V' (experimental LTX/Wan), 'KEN_BURNS' (pan & zoom 2D)

ALTER TABLE series ADD COLUMN animation_mode VARCHAR(50) NOT NULL DEFAULT 'PUPPET_2D';

UPDATE series SET animation_mode = 'PUPPET_2D' WHERE animation_mode IS NULL;
