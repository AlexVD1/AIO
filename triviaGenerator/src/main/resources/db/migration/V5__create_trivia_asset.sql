-- ============================================================
-- V5: Assets (imágenes PNG) de cada trivia
--
-- Cada trivia tiene exactamente 2 assets después de la Fase 11:
--   tipo = 'PREGUNTA'  → imagen sin respuesta correcta revelada
--   tipo = 'RESPUESTA' → imagen con respuesta correcta destacada
--
-- SEPARACIÓN path / url:
--   - path: dónde está el archivo (relativo al storage backend)
--   - url:  cómo se accede desde el exterior
--
--   Esta separación permite cambiar el backend sin afectar los clientes:
--   El cliente siempre usa 'url'.
--   AssetStorageService maneja 'path'.
-- ============================================================

CREATE TABLE trivia_asset (
    id        UUID        NOT NULL DEFAULT gen_random_uuid(),
    trivia_id UUID        NOT NULL,
    tipo      VARCHAR(15) NOT NULL,
    path      TEXT        NOT NULL,
    url       TEXT        NOT NULL,
    mime_type VARCHAR(50) NOT NULL DEFAULT 'image/png',
    created_at TIMESTAMP  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_trivia_asset PRIMARY KEY (id),
    CONSTRAINT fk_trivia_asset_trivia FOREIGN KEY (trivia_id) REFERENCES trivia(id)
);

CREATE INDEX idx_trivia_asset_trivia ON trivia_asset(trivia_id);

COMMENT ON TABLE trivia_asset IS 'Imágenes PNG generadas por trivia. Cada trivia tiene exactamente 2 (PREGUNTA y RESPUESTA).';
COMMENT ON COLUMN trivia_asset.path IS 'Ruta relativa en filesystem o clave en S3/R2. Opaco para el cliente.';
COMMENT ON COLUMN trivia_asset.url IS 'URL pública accesible desde internet. Devuelta en las respuestas JSON.';
