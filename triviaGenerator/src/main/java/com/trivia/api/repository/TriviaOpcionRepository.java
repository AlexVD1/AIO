package com.trivia.api.repository;

import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaOpcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para las opciones de respuesta de una trivia.
 *
 * Las opciones se cargan principalmente junto con su trivia.
 * No se paginan porque una trivia tiene máximo 8 opciones.
 */
@Repository
public interface TriviaOpcionRepository extends JpaRepository<TriviaOpcion, UUID> {

    /**
     * Todas las opciones de una trivia, ordenadas por letra.
     * El orden A, B, C, D es importante para el renderer (Fase 11).
     */
    List<TriviaOpcion> findByTriviaOrderByLetraAsc(Trivia trivia);

    /** Cuenta las opciones correctas de una trivia. Debe ser exactamente 1. */
    long countByTriviaAndCorrectaTrue(Trivia trivia);
}
