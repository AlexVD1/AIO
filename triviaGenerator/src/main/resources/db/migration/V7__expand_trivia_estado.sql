-- ========================================================
-- Migración V7: Ampliar longitud de columna estado en trivia
-- Soporta nuevos estados del ciclo de vida como DESCARGADA_Y_EXPORTADA (22 chars)
-- ========================================================

ALTER TABLE trivia ALTER COLUMN estado TYPE VARCHAR(35);

COMMENT ON COLUMN trivia.estado IS 'ACTIVA | DESCARGADA | EXPORTADA | DESCARGADA_Y_EXPORTADA | INVALIDA | ARCHIVADA';
