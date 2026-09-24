package com.trivia.api.service;

import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.EstadoTrivia;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.dto.StatsResponse;
import com.trivia.api.dto.TriviaResponse;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaGenerationRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Servicio encargado de las consultas de lectura sobre el repositorio persistente de trivias.
 *
 * NOTA ARQUITECTÓNICA:
 *   Este servicio NO interactúa con la Inteligencia Artificial.
 *   Todas las operaciones son consultas directas a la base de datos PostgreSQL,
 *   optimizadas con transacciones de solo lectura (@Transactional(readOnly = true)).
 */
@Service
@Transactional(readOnly = true)
public class TriviaQueryService {

    private final TriviaRepository triviaRepository;
    private final TriviaOpcionRepository triviaOpcionRepository;
    private final TriviaAssetRepository triviaAssetRepository;
    private final TriviaGenerationRepository generationRepository;

    public TriviaQueryService(
            TriviaRepository triviaRepository,
            TriviaOpcionRepository triviaOpcionRepository,
            TriviaAssetRepository triviaAssetRepository,
            TriviaGenerationRepository generationRepository) {
        this.triviaRepository = triviaRepository;
        this.triviaOpcionRepository = triviaOpcionRepository;
        this.triviaAssetRepository = triviaAssetRepository;
        this.generationRepository = generationRepository;
    }

    /**
     * Obtiene una trivia por su identificador único UUID.
     *
     * @param id Identificador de la trivia
     * @return Detalle completo de la trivia con opciones y assets
     * @throws NoSuchElementException si no existe
     */
    public TriviaResponse obtenerPorId(UUID id) {
        Trivia trivia = triviaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trivia no encontrada con ID: " + id));

        return construirTriviaResponse(trivia);
    }

    /**
     * Obtiene una trivia activa seleccionada aleatoriamente del repositorio.
     *
     * @return Detalle de la trivia aleatoria
     * @throws NoSuchElementException si no hay trivias activas
     */
    public TriviaResponse obtenerAleatoria() {
        Trivia trivia = triviaRepository.findRandom()
                .orElseThrow(() -> new NoSuchElementException("No existen trivias activas en el repositorio."));

        return construirTriviaResponse(trivia);
    }

    /**
     * Realiza una búsqueda por coincidencia de texto en la pregunta y la explicación.
     *
     * @param query Texto a buscar
     * @param pageable Configuración de paginación
     * @return Página de resultados
     */
    public Page<TriviaResponse> buscarPorTexto(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return triviaRepository.findAll(pageable).map(this::construirTriviaResponse);
        }

        return triviaRepository.searchByTexto(query.trim(), pageable)
                .map(this::construirTriviaResponse);
    }

    /**
     * Lista todas las trivias paginadas.
     *
     * @param pageable Configuración de paginación
     * @return Página de trivias
     */
    public Page<TriviaResponse> listar(Pageable pageable) {
        return triviaRepository.findAll(pageable)
                .map(this::construirTriviaResponse);
    }

    /**
     * Obtiene estadísticas consolidadas del sistema.
     *
     * @return Métricas globales
     */
    public StatsResponse obtenerEstadisticas() {
        long totalActivas = triviaRepository.countByEstado(EstadoTrivia.ACTIVA);
        long totalGeneraciones = generationRepository.count();
        Double avgIntentos = generationRepository.avgIntentosByEstadoCompletada();
        Double avgDescartadas = generationRepository.avgCantidadDescartadaByEstadoCompletada();

        Map<String, Long> porDificultad = new LinkedHashMap<>();
        for (Dificultad d : Dificultad.values()) {
            porDificultad.put(d.name(), triviaRepository.countByDificultadAndEstado(d, EstadoTrivia.ACTIVA));
        }

        return new StatsResponse(
                totalActivas,
                totalGeneraciones,
                avgIntentos,
                avgDescartadas,
                porDificultad
        );
    }

    private TriviaResponse construirTriviaResponse(Trivia trivia) {
        List<TriviaOpcion> opciones = triviaOpcionRepository.findByTriviaOrderByLetraAsc(trivia);
        List<TriviaAsset> assets = triviaAssetRepository.findByTrivia(trivia);
        return TriviaResponse.from(trivia, opciones, assets);
    }
}
