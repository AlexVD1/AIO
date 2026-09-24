-- ============================================================
-- V4: Opciones de respuesta de cada trivia
--
-- Relación: trivia (1) → trivia_opcion (2..8)
--
-- CONSTRAINT uq_trivia_opcion_trivia_letra:
--   Garantiza que no haya dos opciones con la misma letra dentro de una trivia.
--   Ejemplo inválido:
--     trivia_id=X, letra='A', texto='Venus'
--     trivia_id=X, letra='A', texto='Mercurio'  ← PostgreSQL rechaza esto
--
-- El campo 'correcta' no tiene restricción CHECK de "exactamente uno=true"
-- porque esa regla la aplica TriviaValidator antes de insertar.
-- Una restricción CHECK en PostgreSQL requería una función deferrable complex.
-- ============================================================

CREATE TABLE trivia_opcion (
    id        UUID        NOT NULL DEFAULT gen_random_uuid(),
    trivia_id UUID        NOT NULL,
    letra     VARCHAR(1)  NOT NULL,
    texto     TEXT        NOT NULL,
    correcta  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_trivia_opcion PRIMARY KEY (id),
    CONSTRAINT fk_trivia_opcion_trivia FOREIGN KEY (trivia_id) REFERENCES trivia(id),
    CONSTRAINT uq_trivia_opcion_trivia_letra UNIQUE (trivia_id, letra)
);

CREATE INDEX idx_trivia_opcion_trivia ON trivia_opcion(trivia_id);

COMMENT ON TABLE trivia_opcion IS 'Opciones de respuesta de una trivia. Cada trivia tiene entre 2 y 8 opciones.';
COMMENT ON COLUMN trivia_opcion.letra IS 'A, B, C, D, E, F, G o H. Única por trivia.';
COMMENT ON COLUMN trivia_opcion.correcta IS 'Exactamente una opción por trivia debe ser true. Validado en TriviaValidator.';
