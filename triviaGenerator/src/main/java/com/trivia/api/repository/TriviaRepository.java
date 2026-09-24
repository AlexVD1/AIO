package com.trivia.api.repository;

import com.trivia.api.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio central del sistema: acceso al repositorio permanente de trivias.
 *
 * PRINCIPIOS CLAVE:
 *
 * 1. CONSULTAS SIN IA: Todos estos métodos leen la BD directamente.
 *    Ninguno llama al LLM. La IA solo se invoca en TriviaGenerationService.
 *
 * 2. DEDUPLICACIÓN: existsByPreguntaHash y findByPreguntaHash son los métodos
 *    críticos de la Fase 6. Son rápidos gracias al índice UNIQUE en pregunta_hash.
 *
 * 3. PAGINACIÓN: Los métodos que retornan Page<Trivia> permiten navegar grandes
 *    volúmenes sin cargar toda la tabla en memoria.
 *
 * 4. CONTEXTO PARA LLM: findContextoExistente devuelve preguntas ya existentes
 *    que se incluyen en el prompt del LLM (Fase 7) para evitar duplicados.
 */
@Repository
public interface TriviaRepository extends JpaRepository<Trivia, UUID> {

    // ========================================================
    // DEDUPLICACIÓN (Fase 5 y 6)
    // ========================================================

    /**
     * Verifica si ya existe una trivia con este hash.
     * Método más importante del sistema: impide duplicados exactos.
     * El índice UNIQUE en pregunta_hash hace esta operación O(log n).
     */
    boolean existsByPreguntaHash(String preguntaHash);

    /** Obtiene la trivia por su hash. Útil para reportar qué pregunta es duplicada. */
    Optional<Trivia> findByPreguntaHash(String preguntaHash);

    // ========================================================
    // CONSULTAS DE REPOSITORIO (Fase 9)
    // ========================================================

    /** Trivias activas de un tipo, dificultad e idioma específicos. Paginado. */
    Page<Trivia> findByTipoTriviaAndDificultadAndIdiomaAndEstado(
            TipoTrivia tipoTrivia, Dificultad dificultad,
            String idioma, EstadoTrivia estado, Pageable pageable);

    /** Trivias activas de un tipo e idioma. Paginado. */
    Page<Trivia> findByTipoTriviaAndIdiomaAndEstado(
            TipoTrivia tipoTrivia, String idioma, EstadoTrivia estado, Pageable pageable);

    /** Búsqueda de texto en pregunta o explicación (GET /trivias/search?q=...) */
    @Query("SELECT t FROM Trivia t WHERE t.estado = 'ACTIVA' AND " +
           "(LOWER(t.pregunta) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(t.explicacion) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Trivia> searchByTexto(@Param("q") String query, Pageable pageable);

    /**
     * Una trivia aleatoria activa.
     * ORDER BY RANDOM() funciona en PostgreSQL. No es eficiente para tablas muy grandes
     * (escanea toda la tabla), pero es aceptable para el volumen inicial del sistema.
     * En Fase 9 puede optimizarse si el volumen lo requiere.
     */
    @Query(value = "SELECT * FROM trivia WHERE estado = 'ACTIVA' ORDER BY RANDOM() LIMIT 1",
           nativeQuery = true)
    Optional<Trivia> findRandom();

    // ========================================================
    // CONTEXTO PARA EL LLM (Fase 7)
    // ========================================================

    /**
     * Obtiene preguntas existentes para incluir en el prompt del LLM.
     * Filtra por tipo, dificultad, idioma y subtema para ser relevante.
     * LIMIT evita enviar toda la BD al LLM.
     *
     * Se retornan solo las preguntas (no las opciones ni explicaciones)
     * para mantener el prompt conciso y dentro del límite de tokens.
     */
    @Query("SELECT t.pregunta FROM Trivia t WHERE t.tipoTrivia = :tipoTrivia " +
           "AND t.dificultad = :dificultad AND t.idioma = :idioma " +
           "AND t.estado = 'ACTIVA' " +
           "ORDER BY t.createdAt DESC")
    List<String> findPreguntasContexto(
            @Param("tipoTrivia") TipoTrivia tipoTrivia,
            @Param("dificultad") Dificultad dificultad,
            @Param("idioma") String idioma,
            Pageable pageable);

    // ========================================================
    // ESTADÍSTICAS (Fase 9)
    // ========================================================

    long countByEstado(EstadoTrivia estado);

    long countByTipoTriviaAndEstado(TipoTrivia tipoTrivia, EstadoTrivia estado);

    long countByDificultadAndEstado(Dificultad dificultad, EstadoTrivia estado);

    long countByIdiomaAndEstado(String idioma, EstadoTrivia estado);
}
