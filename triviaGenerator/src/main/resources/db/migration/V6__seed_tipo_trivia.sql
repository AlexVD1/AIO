-- ============================================================
-- V6: Datos iniciales del catálogo de tipos de trivia
--
-- Por qué en Flyway y no como @Bean de Java?
--   1. El catálogo existe independientemente de la app Java.
--   2. Los datos son visibles y modificables sin recompilar.
--   3. Flyway garantiza que se ejecuta exactamente una vez.
--   4. Mantiene la coherencia entre migraciones de schema y datos.
--
-- ON CONFLICT DO NOTHING:
--   Si la migración se ejecuta nuevamente (ej: en tests), no falla.
--   Aunque Flyway normalmente impide esto con su tabla de versiones.
-- ============================================================

INSERT INTO tipo_trivia (codigo, nombre, descripcion) VALUES
    ('CIENCIA_NATURAL',
     'Ciencias Naturales',
     'Biología, química y física general para todos los niveles'),

    ('GEOLOGIA',
     'Geología',
     'Rocas, minerales, tectónica de placas, volcanes y terremotos'),

    ('ASTRONOMIA',
     'Astronomía',
     'Planetas, estrellas, galaxias, cosmología y exploración espacial'),

    ('HISTORIA',
     'Historia',
     'Historia universal, grandes civilizaciones y eventos históricos clave'),

    ('GEOGRAFIA',
     'Geografía',
     'Países, capitales, ríos, montañas, océanos y accidentes geográficos'),

    ('TECNOLOGIA',
     'Tecnología',
     'Informática, inventos, innovación tecnológica y sistemas digitales'),

    ('MATEMATICAS',
     'Matemáticas',
     'Aritmética, álgebra, geometría, estadística y lógica matemática'),

    ('ANIMALES',
     'Animales',
     'Zoología, comportamiento animal, especies, hábitats y biodiversidad'),

    ('CULTURA_GENERAL',
     'Cultura General',
     'Conocimiento general diverso: arte, literatura, gastronomía, deportes y más')

ON CONFLICT (codigo) DO NOTHING;
