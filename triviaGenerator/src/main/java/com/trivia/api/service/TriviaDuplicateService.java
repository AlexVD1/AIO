package com.trivia.api.service;

import com.trivia.api.dto.DeduplicationResult;
import com.trivia.api.repository.TriviaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

/**
 * Servicio encargado de la detección y filtrado de trivias duplicadas.
 *
 * ESTRATEGIA DE DEDUPLICACIÓN EN CAPAS (Fase 6):
 *   Capa 1: Normalización de texto (TriviaQuestionNormalizer)
 *   Capa 2: SHA-256 Hash exacto (QuestionHashService)
 *   Capa 3: Deduplicación intra-lote (dentro del mismo batch generado por el LLM)
 *   Capa 4: Deduplicación contra el histórico persistente en PostgreSQL (TriviaRepository)
 *   Capa 5: (Fase 10) Similitud semántica con pgvector embeddings
 *
 * OBSERVABILIDAD:
 *   El servicio clasifica los descartes en intra-lote y base de datos para alimentar
 *   los contadores de 'cantidadDescartada' en TriviaGeneration.
 */
@Service
@Transactional(readOnly = true)
public class TriviaDuplicateService {

    private final QuestionHashService hashService;
    private final TriviaRepository triviaRepository;

    public TriviaDuplicateService(QuestionHashService hashService,
                                  TriviaRepository triviaRepository) {
        this.hashService = hashService;
        this.triviaRepository = triviaRepository;
    }

    /**
     * Verifica si una pregunta ya existe en la base de datos (por su texto original).
     *
     * @param rawQuestion Texto de la pregunta
     * @return true si ya existe una trivia con el mismo hash en la base de datos
     */
    public boolean existePreguntaEnBaseDeDatos(String rawQuestion) {
        String hash = hashService.hashQuestion(rawQuestion);
        return existePorHash(hash);
    }

    /**
     * Verifica si un hash SHA-256 ya existe en la base de datos.
     * Operación O(log n) gracias al índice UNIQUE en pregunta_hash.
     *
     * @param preguntaHash Hash de 64 caracteres
     * @return true si ya existe
     */
    public boolean existePorHash(String preguntaHash) {
        return triviaRepository.existsByPreguntaHash(preguntaHash);
    }

    /**
     * Calcula el hash normalizado de una pregunta.
     *
     * @param rawQuestion Texto de la pregunta
     * @return Hash SHA-256
     */
    public String calcularHash(String rawQuestion) {
        return hashService.hashQuestion(rawQuestion);
    }

    /**
     * Normaliza el texto de una pregunta.
     *
     * @param rawQuestion Texto original
     * @return Texto normalizado
     */
    public String normalizar(String rawQuestion) {
        return hashService.normalize(rawQuestion);
    }

    /**
     * Deduplica una lista de elementos candidatos evaluando:
     * 1. Duplicados dentro del mismo lote (el primer elemento único se acepta; repetidos se descartan).
     * 2. Duplicados contra el repositorio histórico en base de datos.
     *
     * @param elementos Lista de elementos candidatos
     * @param extractorPregunta Función para obtener el texto de la pregunta de cada elemento
     * @param <T> Tipo del elemento
     * @return Resultado estructurado con elementos válidos y descartados por causa
     */
    public <T> DeduplicationResult<T> deduplicar(List<T> elementos, Function<T, String> extractorPregunta) {
        return deduplicar(elementos, extractorPregunta, Collections.emptySet());
    }

    /**
     * Deduplica una lista de elementos candidatos considerando también un conjunto de hashes
     * previamente acumulados en intentos anteriores de la misma sesión de generación.
     *
     * @param elementos Lista de elementos candidatos
     * @param extractorPregunta Función para obtener el texto de la pregunta
     * @param hashesSesion Hashes ya aceptados en la sesión actual
     * @param <T> Tipo del elemento
     * @return DeduplicationResult con clasificación detallada
     */
    public <T> DeduplicationResult<T> deduplicar(List<T> elementos,
                                                Function<T, String> extractorPregunta,
                                                Set<String> hashesSesion) {
        if (elementos == null || elementos.isEmpty()) {
            return new DeduplicationResult<>(List.of(), List.of(), List.of());
        }

        List<T> validos = new ArrayList<>();
        List<T> descartadosPorBatch = new ArrayList<>();
        List<T> descartadosPorBaseDeDatos = new ArrayList<>();

        // Rastrear hashes vistos durante esta evaluación (incluyendo previos de la sesión)
        Set<String> hashesVistos = new HashSet<>(hashesSesion);

        for (T elemento : elementos) {
            String rawQuestion = extractorPregunta.apply(elemento);
            String hash = hashService.hashQuestion(rawQuestion);

            // 1. Revisar duplicado intra-lote / intra-sesión
            if (hashesVistos.contains(hash)) {
                descartadosPorBatch.add(elemento);
                continue;
            }

            // 2. Revisar duplicado contra la base de datos persistente
            if (triviaRepository.existsByPreguntaHash(hash)) {
                descartadosPorBaseDeDatos.add(elemento);
                hashesVistos.add(hash); // Registrar para no volver a consultar BD en el mismo lote
                continue;
            }

            // Es única y válida
            hashesVistos.add(hash);
            validos.add(elemento);
        }

        return new DeduplicationResult<>(validos, descartadosPorBatch, descartadosPorBaseDeDatos);
    }
}
