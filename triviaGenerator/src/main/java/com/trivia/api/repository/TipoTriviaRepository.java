package com.trivia.api.repository;

import com.trivia.api.domain.TipoTrivia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para el catálogo de tipos de trivia.
 *
 * Spring Data JPA genera automáticamente la implementación de esta interfaz.
 * Los nombres de los métodos siguen convenciones que Spring interpreta como queries:
 *   findBy[Campo] → SELECT ... WHERE campo = ?
 *   existsBy[Campo] → SELECT COUNT(*) > 0 WHERE campo = ?
 *
 * Por qué extiende JpaRepository y no CrudRepository?
 *   JpaRepository agrega métodos de paginación (findAll(Pageable))
 *   que usaremos en la Fase 9 para los endpoints de consulta.
 */
@Repository
public interface TipoTriviaRepository extends JpaRepository<TipoTrivia, UUID> {

    /**
     * Busca un tipo de trivia por su código de negocio.
     * Usado en validación del request: ¿existe "ASTRONOMIA" en el catálogo?
     */
    Optional<TipoTrivia> findByCodigo(String codigo);

    Optional<TipoTrivia> findByCodigoIgnoreCase(String codigo);

    Optional<TipoTrivia> findByNombreIgnoreCase(String nombre);

    /**
     * Obtiene solo los tipos activos.
     * Usado en GET /api/v1/catalogos/tipos-trivia.
     */
    List<TipoTrivia> findByActivoTrue();

    /**
     * Verifica si existe un tipo con ese código.
     * Más eficiente que findByCodigo cuando solo necesitamos boolean.
     */
    boolean existsByCodigo(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
