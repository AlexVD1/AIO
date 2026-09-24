package com.trivia.api.repository;

import com.trivia.api.domain.EstadoGeneracion;
import com.trivia.api.domain.TriviaGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para TriviaGeneration.
 *
 * Permite consultar el historial de generaciones por estado,
 * y obtener estadísticas para el endpoint /stats.
 */
@Repository
public interface TriviaGenerationRepository extends JpaRepository<TriviaGeneration, UUID> {

    /** Todas las generaciones en un estado dado. Útil para monitoreo. */
    List<TriviaGeneration> findByEstado(EstadoGeneracion estado);

    /** Conteo de generaciones por estado. Útil para estadísticas. */
    long countByEstado(EstadoGeneracion estado);

    /**
     * Promedio de intentos por generación completada.
     * @Query con JPQL (Java Persistence Query Language):
     *   - Opera sobre entidades Java, no tablas SQL
     *   - "g" es el alias de TriviaGeneration
     *   - Portable entre bases de datos (aunque usamos PostgreSQL)
     */
    @Query("SELECT AVG(g.intentos) FROM TriviaGeneration g WHERE g.estado = 'COMPLETADA'")
    Double avgIntentosByEstadoCompletada();

    /**
     * Promedio de trivias descartadas por generación completada.
     * Indica qué tan "dura" es la deduplicación en promedio.
     */
    @Query("SELECT AVG(g.cantidadDescartada) FROM TriviaGeneration g WHERE g.estado = 'COMPLETADA'")
    Double avgCantidadDescartadaByEstadoCompletada();
}
