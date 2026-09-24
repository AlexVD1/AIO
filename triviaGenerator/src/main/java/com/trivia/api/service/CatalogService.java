package com.trivia.api.service;

import com.trivia.api.domain.TipoTrivia;
import com.trivia.api.dto.TipoTriviaResponse;
import com.trivia.api.repository.TipoTriviaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para operaciones del catálogo de tipos de trivia.
 *
 * Por qué existe esta capa si solo llama al repositorio?
 *   1. SEPARACIÓN DE RESPONSABILIDADES: El controller no conoce la lógica de negocio.
 *   2. TRANSACCIONES: @Transactional(readOnly = true) optimiza consultas de solo lectura:
 *      - Hibernate no crea snapshot del objeto para detectar cambios (dirty checking)
 *      - La BD puede usar réplicas de lectura si están configuradas
 *      - El connection pool puede reciclar la conexión antes
 *   3. EXTENSIBILIDAD: Si en el futuro queremos caché (@Cacheable), logging,
 *      validaciones o transformaciones complejas, ya tienen su lugar.
 *
 * Por qué @Transactional(readOnly = true) y no solo @Transactional?
 *   readOnly = true indica a Hibernate y la BD que esta operación no modifica datos.
 *   Esto permite optimizaciones automáticas sin cambiar el código de negocio.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final TipoTriviaRepository tipoTriviaRepository;

    // Inyección por constructor (NO @Autowired en el campo):
    // - Permite tests sin Spring (pasar mock directamente en el constructor)
    // - Hace las dependencias explícitas y visibles
    // - IntelliJ/SonarLint no emiten warning de "field injection"
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogService.class);

    public CatalogService(TipoTriviaRepository tipoTriviaRepository) {
        this.tipoTriviaRepository = tipoTriviaRepository;
    }

    /**
     * Retorna todos los tipos de trivia activos como DTOs.
     *
     * @return Lista de tipos activos, vacía si no hay ninguno
     */
    public List<TipoTriviaResponse> obtenerTiposActivos() {
        return tipoTriviaRepository.findByActivoTrue()
                .stream()
                .map(TipoTriviaResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Valida y obtiene un tipo de trivia a partir de la entrada del usuario.
     *
     * COMPORTAMIENTO INTELIGENTE:
     *   1. Si coincide exactamente o es muy similar (fuzzy match, acentos, plural/singular,
     *      distancia de Levenshtein) con una categoría existente en la BD:
     *      -> Reutiliza la categoría existente (evitando duplicados o typos como 'Historias' o 'Hstoria').
     *   2. Si NO existe ninguna categoría similar:
     *      -> Auto-registra la nueva categoría dinámicamente en la tabla 'tipo_trivia' de PostgreSQL
     *         y la devuelve para su uso inmediato.
     *
     * @param entrada Código o nombre ingresado por el usuario (ej: "ASTRONOMIA", "cine", "Filosofía")
     * @return La entidad TipoTrivia correspondiente (existente o recién creada)
     */
    @Transactional
    public TipoTrivia validarYObtenerTipo(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new IllegalArgumentException("El tipo de trivia es obligatorio y no puede estar vacío.");
        }

        String inputLimpio = entrada.trim();

        // 1. Obtener todas las categorías activas existentes en la base de datos
        List<TipoTrivia> existentes = tipoTriviaRepository.findByActivoTrue();

        // 2. Buscar si coincide o es muy similar a alguna existente
        for (TipoTrivia existente : existentes) {
            if (CategorySimilarityMatcher.esSimilar(inputLimpio, existente)) {
                log.info("Entrada de tipo de trivia '{}' mapeada a categoría existente [{}] '{}'",
                        inputLimpio, existente.getCodigo(), existente.getNombre());
                return existente;
            }
        }

        // 3. No existe ninguna categoría similar: Auto-creación dinámica
        String codigoNuevo = CategorySimilarityMatcher.toCanonicalSlug(inputLimpio);
        if (codigoNuevo.isEmpty()) {
            codigoNuevo = "GENERAL";
        }
        if (codigoNuevo.length() > 50) {
            codigoNuevo = codigoNuevo.substring(0, 50);
        }

        String nombreNuevo = CategorySimilarityMatcher.toTitleCase(inputLimpio);
        if (nombreNuevo.length() > 100) {
            nombreNuevo = nombreNuevo.substring(0, 100);
        }

        String descripcion = "Categoría autogenerada para trivias de " + nombreNuevo;
        TipoTrivia nuevoTipo = new TipoTrivia(codigoNuevo, nombreNuevo, descripcion);

        try {
            TipoTrivia persistido = tipoTriviaRepository.saveAndFlush(nuevoTipo);
            log.info("Nueva categoría de trivia auto-registrada exitosamente en BD: [{}] '{}'",
                    codigoNuevo, nombreNuevo);
            return persistido;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Manejo de condición de carrera concurrente (otro hilo insertó el mismo código al mismo instante)
            return tipoTriviaRepository.findByCodigoIgnoreCase(codigoNuevo)
                    .orElseThrow(() -> ex);
        }
    }
}
