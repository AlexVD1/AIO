-- ============================================================
-- V3: Tabla principal de trivias
--
-- Esta tabla ES LA MEMORIA PERMANENTE DEL SISTEMA.
-- Nunca se eliminan registros automáticamente.
-- Las trivias inválidas o archivadas cambian su estado pero permanecen.
--
-- CONSTRAINT ÚNICO MÁS IMPORTANTE DEL SISTEMA:
--   uq_trivia_pregunta_hash
--   Garantiza que no existan dos trivias con la misma pregunta normalizada,
--   incluso en solicitudes concurrentes (race condition).
--
--   Ejemplo del problema que resuelve:
--     Thread A: SELECT → no existe "¿Cuál es el planeta más cercano al Sol?"
--     Thread B: SELECT → no existe "¿Cuál es el planeta más cercano al Sol?"
--     Thread A: INSERT → OK (primera inserción)
--     Thread B: INSERT → FALLA con unique_violation (PostgreSQL rechaza el duplicado)
--
--   La aplicación debe capturar este error y manejarlo gracefully.
--
-- EMBEDDING (Fase 10):
--   La columna embedding es TEXT ahora como placeholder.
--   En la Fase 10 se ejecutará una migración V10 que:
--     1. Instalará la extensión pgvector: CREATE EXTENSION IF NOT EXISTS vector;
--     2. Cambiará el tipo: ALTER TABLE trivia ALTER COLUMN embedding TYPE vector(1536)
--        USING embedding::vector;
--   Esto NO requiere cambios en las entidades JPA ya existentes.
-- ============================================================

CREATE TABLE trivia (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    generation_id        UUID        NOT NULL,
    tipo_trivia_id       UUID        NOT NULL,
    pregunta             TEXT        NOT NULL,
    pregunta_normalizada TEXT        NOT NULL,
    pregunta_hash        VARCHAR(64) NOT NULL,
    embedding            TEXT,           -- Fase 10: cambiará a vector(1536) con pgvector
    explicacion          TEXT        NOT NULL,
    dificultad           VARCHAR(10) NOT NULL,
    idioma               VARCHAR(10) NOT NULL,
    subtema              VARCHAR(200),
    estado               VARCHAR(15) NOT NULL DEFAULT 'ACTIVA',
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_trivia             PRIMARY KEY (id),
    CONSTRAINT uq_trivia_pregunta_hash UNIQUE (pregunta_hash),
    CONSTRAINT fk_trivia_generation  FOREIGN KEY (generation_id) REFERENCES trivia_generation(id),
    CONSTRAINT fk_trivia_tipo        FOREIGN KEY (tipo_trivia_id) REFERENCES tipo_trivia(id)
);

-- Índices para las consultas del repositorio (GET /api/v1/trivias?...)
CREATE INDEX idx_trivia_tipo_trivia  ON trivia(tipo_trivia_id);
CREATE INDEX idx_trivia_dificultad   ON trivia(dificultad);
CREATE INDEX idx_trivia_idioma       ON trivia(idioma);
CREATE INDEX idx_trivia_estado       ON trivia(estado);
CREATE INDEX idx_trivia_subtema      ON trivia(subtema);
CREATE INDEX idx_trivia_created_at   ON trivia(created_at DESC);

-- Índice compuesto para la consulta de contexto del LLM
-- (tipo + dificultad + idioma es el filtro más común)
CREATE INDEX idx_trivia_contexto     ON trivia(tipo_trivia_id, dificultad, idioma, estado);

COMMENT ON TABLE trivia IS 'Repositorio permanente de trivias. La BD es la memoria del sistema.';
COMMENT ON COLUMN trivia.pregunta_hash IS 'SHA-256(pregunta_normalizada) en hex. 64 chars. UNIQUE = barrera final contra duplicados exactos';
COMMENT ON COLUMN trivia.embedding IS 'Fase 10: será vector(1536) con pgvector para deduplicación semántica';
COMMENT ON COLUMN trivia.estado IS 'ACTIVA | INVALIDA | ARCHIVADA. Las trivias no se eliminan, se archivan.';
