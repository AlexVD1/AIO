-- ============================================================
-- V1: Catálogo de tipos de trivia
--
-- Esta es la tabla raíz del sistema.
-- Todas las trivias y generaciones referencian un tipo de trivia.
--
-- gen_random_uuid(): función nativa de PostgreSQL 13+ que genera
-- un UUID v4 aleatorio. Equivalente a UUID.randomUUID() en Java.
-- Se usa como DEFAULT para que PostgreSQL genere el UUID automáticamente
-- en cualquier INSERT que no especifique el id.
--
-- TIMESTAMPTZ: "timestamp with time zone" almacena la fecha/hora
-- en UTC internamente, independientemente de la zona horaria del servidor.
-- Esto es importante para portabilidad y consistencia.
-- ============================================================

CREATE TABLE tipo_trivia (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    codigo      VARCHAR(50) NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tipo_trivia PRIMARY KEY (id),
    CONSTRAINT uq_tipo_trivia_codigo UNIQUE (codigo)
);

COMMENT ON TABLE tipo_trivia IS 'Catálogo de categorías de trivia disponibles';
COMMENT ON COLUMN tipo_trivia.codigo IS 'Identificador de negocio: ASTRONOMIA, GEOLOGIA, etc.';
COMMENT ON COLUMN tipo_trivia.activo IS 'Si false, el tipo no aparece en el catálogo pero sus trivias se conservan';
