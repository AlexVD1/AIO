package com.trivia.api.service;

import com.trivia.api.domain.TriviaGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Ejecutor asíncrono para el pipeline de generación de trivias.
 *
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   Al delegar la anotación @Async a este componente independiente, se garantiza
 *   que las llamadas pasen a través del proxy AOP de Spring, ejecutándose en el
 *   pool de hilos de fondo sin bloquear el hilo HTTP que retorna el código 202 Accepted.
 */
@Service
public class AsyncTriviaPipelineExecutor {

    private static final Logger log = LoggerFactory.getLogger(AsyncTriviaPipelineExecutor.class);

    private final TriviaGenerationService generationService;

    public AsyncTriviaPipelineExecutor(TriviaGenerationService generationService) {
        this.generationService = generationService;
    }

    /**
     * Ejecuta el pipeline de generación en segundo plano para una solicitud ya registrada.
     *
     * @param generationId Identificador de la generación
     * @return CompletableFuture con el resultado final
     */
    @Async
    public CompletableFuture<TriviaGeneration> ejecutarPipelineAsync(UUID generationId) {
        log.info("Iniciando procesamiento asíncrono en background thread para generación {}", generationId);
        try {
            TriviaGeneration result = generationService.ejecutarPipelineDesdeId(generationId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Excepción en ejecución asíncrona de generación {}: {}", generationId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
