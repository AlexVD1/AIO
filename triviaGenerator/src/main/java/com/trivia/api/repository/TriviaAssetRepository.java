package com.trivia.api.repository;

import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TipoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para los assets (imágenes PNG) de una trivia.
 *
 * Cada trivia tiene exactamente dos assets: PREGUNTA y RESPUESTA.
 * Este repositorio permite recuperar ambos para incluir las URLs
 * en las respuestas del endpoint de consulta.
 */
@Repository
public interface TriviaAssetRepository extends JpaRepository<TriviaAsset, UUID> {

    /**
     * Todos los assets de una trivia (normalmente 2: PREGUNTA y RESPUESTA).
     * Usado al construir la respuesta del endpoint GET /api/v1/trivias/{id}.
     */
    List<TriviaAsset> findByTrivia(Trivia trivia);

    /** Asset específico por tipo. Útil para obtener solo la imagen de pregunta. */
    Optional<TriviaAsset> findByTriviaAndTipo(Trivia trivia, TipoAsset tipo);
}
