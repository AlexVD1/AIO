-- ============================================================
-- V2: Registro de solicitudes de generación
--
-- Cada POST /api/v1/trivias crea un registro aquí.
-- Sirve para:
--   1. Trazabilidad: qué se pidió, cuándo, con qué parámetros
--   2. Observabilidad: cuánto tardó, cuántas se descartaron
--   3. Estado: el cliente puede consultar GET /api/v1/trivias/{id}/status
--
-- Los campos cantidad_* son contadores que se actualizan durante el pipeline:
--   cantidad_generada:  cuántas generó el LLM en total (todos los intentos)
--   cantidad_descartada: cuántas fueron rechazadas (duplicadas + inválidas)
--   cantidad_final:      cuántas quedaron almacenadas al final
--
-- Invariante de negocio:
--   cantidad_final = cantidad_generada - cantidad_descartada
-- ============================================================

CREATE TABLE trivia_generation (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    tipo_trivia_id       UUID        NOT NULL,
    subtema              VARCHAR(200),
    cantidad_solicitada  INTEGER     NOT NULL,
    cantidad_generada    INTEGER     NOT NULL DEFAULT 0,
    cantidad_descartada  INTEGER     NOT NULL DEFAULT 0,
    cantidad_final       INTEGER     NOT NULL DEFAULT 0,
    numero_opciones      INTEGER     NOT NULL,
    dificultad           VARCHAR(10) NOT NULL,
    idioma               VARCHAR(10) NOT NULL,
    intentos             INTEGER     NOT NULL DEFAULT 0,
    estado               VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    error                TEXT,
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    completed_at         TIMESTAMP,

    CONSTRAINT pk_trivia_generation PRIMARY KEY (id),
    CONSTRAINT fk_trivia_generation_tipo FOREIGN KEY (tipo_trivia_id) REFERENCES tipo_trivia(id)
);

-- Índices para consultas frecuentes
CREATE INDEX idx_trivia_generation_estado     ON trivia_generation(estado);
CREATE INDEX idx_trivia_generation_tipo_trivia ON trivia_generation(tipo_trivia_id);
CREATE INDEX idx_trivia_generation_created_at ON trivia_generation(created_at DESC);

COMMENT ON TABLE trivia_generation IS 'Registro de cada solicitud POST /api/v1/trivias';
COMMENT ON COLUMN trivia_generation.intentos IS 'Número de ciclos LLM realizados. Máximo: MAX_GENERATION_ATTEMPTS';
COMMENT ON COLUMN trivia_generation.error IS 'Mensaje de error si estado = ERROR. NULL si fue exitoso';
