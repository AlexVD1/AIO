package com.trivia.api.service;

import com.trivia.api.ai.TriviaAiClient;
import com.trivia.api.ai.dto.AiGenerationRequest;
import com.trivia.api.ai.dto.AiTriviaItem;
import com.trivia.api.ai.dto.AiTriviaOpcion;
import com.trivia.api.domain.*;
import com.trivia.api.dto.DeduplicationResult;
import com.trivia.api.dto.TriviaGenerationRequest;
import com.trivia.api.dto.TriviaGenerationResponse;
import com.trivia.api.exception.InsufficientUniqueTriviasException;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaGenerationRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Servicio orquestador del pipeline completo de generación de trivias con IA.
 *
 * FLUJO DEL PIPELINE:
 *   1. PENDIENTE: Validación de request y verificación en catálogo (TipoTrivia)
 *   2. GENERANDO: Consulta de contexto previo en BD y solicitud inicial al LLM con buffer
 *   3. VALIDANDO: Validación estructural y de negocio (TriviaValidationService)
 *   4. BUSCANDO_DUPLICADOS: Deduplicación en batch y contra histórico en BD (TriviaDuplicateService)
 *   5. REGENERANDO: Si faltan trivias únicas, reintentos progresivos hasta MAX_GENERATION_ATTEMPTS
 *   6. GUARDANDO: Persistencia transaccional de trivias y opciones en PostgreSQL
 *   7. COMPLETADA: Actualización de métricas finales y finalización
 */
@Service
public class TriviaGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TriviaGenerationService.class);

    private final CatalogService catalogService;
    private final TriviaGenerationRepository generationRepository;
    private final TriviaRepository triviaRepository;
    private final TriviaOpcionRepository triviaOpcionRepository;
    private final TriviaAssetRepository triviaAssetRepository;
    private final TriviaAiClient aiClient;
    private final TriviaValidationService validationService;
    private final TriviaDuplicateService duplicateService;
    private final TriviaRendererService rendererService;
    private final AssetStorageService assetStorageService;
    private final com.trivia.api.ai.EmbeddingService embeddingService;

    private final int bufferPercentage;
    private final int maxAttempts;

    public TriviaGenerationService(
            CatalogService catalogService,
            TriviaGenerationRepository generationRepository,
            TriviaRepository triviaRepository,
            TriviaOpcionRepository triviaOpcionRepository,
            TriviaAssetRepository triviaAssetRepository,
            TriviaAiClient aiClient,
            TriviaValidationService validationService,
            TriviaDuplicateService duplicateService,
            TriviaRendererService rendererService,
            AssetStorageService assetStorageService,
            com.trivia.api.ai.EmbeddingService embeddingService,
            @Value("${trivia.generation.buffer-percentage:50}") int bufferPercentage,
            @Value("${trivia.generation.max-attempts:5}") int maxAttempts) {
        this.catalogService = catalogService;
        this.generationRepository = generationRepository;
        this.triviaRepository = triviaRepository;
        this.triviaOpcionRepository = triviaOpcionRepository;
        this.triviaAssetRepository = triviaAssetRepository;
        this.aiClient = aiClient;
        this.validationService = validationService;
        this.duplicateService = duplicateService;
        this.rendererService = rendererService;
        this.assetStorageService = assetStorageService;
        this.embeddingService = embeddingService;
        this.bufferPercentage = bufferPercentage;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Inicia y orquesta la generación síncrona de trivias.
     *
     * @param request Parámetros de la solicitud
     * @return DTO con estado y resultados de la generación
     */
    @Transactional
    public TriviaGenerationResponse generarTrivias(TriviaGenerationRequest request) {
        TipoTrivia tipoTrivia = catalogService.validarYObtenerTipo(request.tipoTrivia());

        TriviaGeneration generation = new TriviaGeneration(
                tipoTrivia,
                request.cantidad(),
                request.numeroOpciones(),
                request.dificultad(),
                request.idioma(),
                request.subtema()
        );
        generation = generationRepository.save(generation);

        try {
            generation = ejecutarPipeline(generation);
            return TriviaGenerationResponse.from(generation);
        } catch (Exception e) {
            log.error("Fallo durante el pipeline de generación {}: {}", generation.getId(), e.getMessage());
            generation.fallar(e.getMessage());
            generationRepository.save(generation);
            throw e;
        }
    }

    /**
     * Registra una nueva solicitud de generación en estado PENDIENTE.
     *
     * @param request Datos de la solicitud
     * @return DTO con estado PENDIENTE y UUID asignado
     */
    @Transactional
    public TriviaGenerationResponse registrarSolicitudPendiente(TriviaGenerationRequest request) {
        TipoTrivia tipoTrivia = catalogService.validarYObtenerTipo(request.tipoTrivia());

        TriviaGeneration generation = new TriviaGeneration(
                tipoTrivia,
                request.cantidad(),
                request.numeroOpciones(),
                request.dificultad(),
                request.idioma(),
                request.subtema()
        );
        generation = generationRepository.save(generation);
        return TriviaGenerationResponse.from(generation);
    }

    /**
     * Ejecuta el pipeline para una generación existente identificada por su UUID.
     *
     * @param generationId UUID de la generación
     * @return TriviaGeneration finalizada
     */
    @Transactional
    public TriviaGeneration ejecutarPipelineDesdeId(UUID generationId) {
        TriviaGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new NoSuchElementException("Generación no encontrada con ID: " + generationId));
        try {
            return ejecutarPipeline(generation);
        } catch (Exception e) {
            log.error("Fallo durante el pipeline de generación {}: {}", generationId, e.getMessage());
            generation.fallar(e.getMessage());
            return generationRepository.save(generation);
        }
    }

    /**
     * Consulta el estado en vivo de una generación por su identificador.
     *
     * @param generationId UUID de la generación
     * @return DTO con métricas y estado actual
     */
    @Transactional(readOnly = true)
    public TriviaGenerationResponse obtenerEstadoGeneracion(UUID generationId) {
        TriviaGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new NoSuchElementException("Generación no encontrada con ID: " + generationId));
        return TriviaGenerationResponse.from(generation);
    }

    private TriviaGeneration ejecutarPipeline(TriviaGeneration generation) {
        log.info("Iniciando pipeline de generación {} (solicitadas: {}, dificultad: {}, tema: {})",
                generation.getId(), generation.getCantidadSolicitada(),
                generation.getDificultad(), generation.getTipoTrivia().getCodigo());

        generation.setEstado(EstadoGeneracion.GENERANDO);
        generationRepository.save(generation);

        // Consultar memoria previa en BD para inyectar en el prompt
        List<String> contextoPrevio = triviaRepository.findPreguntasContexto(
                generation.getTipoTrivia(),
                generation.getDificultad(),
                generation.getIdioma(),
                PageRequest.of(0, 30)
        );

        List<AiTriviaItem> acumuladasValidas = new ArrayList<>();
        Set<String> hashesAcumulados = new HashSet<>();
        List<String> preguntasAcumuladas = new ArrayList<>(contextoPrevio);

        int totalGeneradasLlm = 0;
        int totalDescartadas = 0;

        while (acumuladasValidas.size() < generation.getCantidadSolicitada() && generation.getIntentos() < maxAttempts) {
            generation.incrementarIntento();

            if (generation.getIntentos() > 1) {
                generation.setEstado(EstadoGeneracion.REGENERANDO);
                generationRepository.save(generation);
                log.info("Reintento {}/{} para generación {}",
                        generation.getIntentos(), maxAttempts, generation.getId());
            }

            int faltantes = generation.getCantidadSolicitada() - acumuladasValidas.size();
            int buffer = (int) Math.ceil(faltantes * (bufferPercentage / 100.0));
            int cantidadLote = faltantes + buffer;

            AiGenerationRequest aiRequest = new AiGenerationRequest(
                    generation.getTipoTrivia().getCodigo(),
                    generation.getSubtema(),
                    generation.getDificultad(),
                    generation.getIdioma(),
                    generation.getNumeroOpciones(),
                    cantidadLote,
                    preguntasAcumuladas
            );

            // 1. Invocar IA
            List<AiTriviaItem> loteGenerado = aiClient.generate(aiRequest);
            totalGeneradasLlm += (loteGenerado != null ? loteGenerado.size() : 0);

            // 2. Validar estructura y lógica
            generation.setEstado(EstadoGeneracion.VALIDANDO);
            List<AiTriviaItem> validasEstructuralmente = validationService.filterValid(
                    loteGenerado, generation.getNumeroOpciones()
            );
            int descartesValidacion = (loteGenerado != null ? loteGenerado.size() : 0) - validasEstructuralmente.size();
            totalDescartadas += descartesValidacion;

            // 3. Deduplicar en batch y contra BD
            generation.setEstado(EstadoGeneracion.BUSCANDO_DUPLICADOS);
            DeduplicationResult<AiTriviaItem> dedupResult = duplicateService.deduplicar(
                    validasEstructuralmente,
                    AiTriviaItem::pregunta,
                    hashesAcumulados
            );
            totalDescartadas += dedupResult.totalDescartados();

            // Incorporar válidas hasta completar la cuota solicitada
            for (AiTriviaItem item : dedupResult.validos()) {
                if (acumuladasValidas.size() < generation.getCantidadSolicitada()) {
                    acumuladasValidas.add(item);
                    String hash = duplicateService.calcularHash(item.pregunta());
                    hashesAcumulados.add(hash);
                    preguntasAcumuladas.add(item.pregunta());
                }
            }

            // Actualizar contadores parciales para observabilidad
            generation.setCantidadGenerada(totalGeneradasLlm);
            generation.setCantidadDescartada(totalDescartadas);
            generationRepository.save(generation);
        }

        // Si se agotaron los intentos sin completar la cantidad solicitada
        if (acumuladasValidas.size() < generation.getCantidadSolicitada()) {
            throw new InsufficientUniqueTriviasException(
                    generation.getCantidadSolicitada(),
                    acumuladasValidas.size(),
                    generation.getIntentos()
            );
        }

        // 4. Guardar en base de datos
        generation.setEstado(EstadoGeneracion.GUARDANDO);
        generationRepository.save(generation);

        List<Trivia> triviasPersistidas = new ArrayList<>();
        Map<Trivia, List<TriviaOpcion>> opcionesPorTrivia = new HashMap<>();

        for (AiTriviaItem item : acumuladasValidas) {
            String normalizada = duplicateService.normalizar(item.pregunta());
            String hash = duplicateService.calcularHash(item.pregunta());

            Trivia trivia = new Trivia(
                    generation,
                    generation.getTipoTrivia(),
                    item.pregunta(),
                    normalizada,
                    hash,
                    item.explicacion(),
                    generation.getDificultad(),
                    generation.getIdioma(),
                    generation.getSubtema()
            );

            // Capa 5: Generar y serializar embedding vectorial
            float[] vectorEmbedding = embeddingService.generateEmbedding(item.pregunta());
            trivia.setEmbedding(embeddingService.serialize(vectorEmbedding));

            trivia = triviaRepository.save(trivia);
            triviasPersistidas.add(trivia);

            List<TriviaOpcion> opcionesGuardadas = new ArrayList<>();
            for (AiTriviaOpcion opc : item.opciones()) {
                TriviaOpcion opcion = new TriviaOpcion(
                        trivia,
                        opc.letra().toUpperCase(Locale.ROOT),
                        opc.texto(),
                        Boolean.TRUE.equals(opc.correcta())
                );
                opcionesGuardadas.add(triviaOpcionRepository.save(opcion));
            }
            opcionesPorTrivia.put(trivia, opcionesGuardadas);
        }

        // 5. Renderizar y almacenar assets (2 PNGs por trivia)
        generation.setEstado(EstadoGeneracion.RENDERIZANDO);
        generationRepository.save(generation);

        for (Trivia trivia : triviasPersistidas) {
            List<TriviaOpcion> opciones = opcionesPorTrivia.get(trivia);

            try {
                // Asset 1: Imagen de PREGUNTA
                byte[] pngPregunta = rendererService.renderPregunta(trivia, opciones);
                String pathPregunta = String.format("%s/%s-pregunta.png", generation.getId(), trivia.getId());
                String urlPregunta = assetStorageService.store(pngPregunta, pathPregunta, "image/png");
                triviaAssetRepository.save(new TriviaAsset(trivia, TipoAsset.PREGUNTA, pathPregunta, urlPregunta));

                // Asset 2: Imagen de RESPUESTA (con respuesta destacada y explicación)
                byte[] pngRespuesta = rendererService.renderRespuesta(trivia, opciones);
                String pathRespuesta = String.format("%s/%s-respuesta.png", generation.getId(), trivia.getId());
                String urlRespuesta = assetStorageService.store(pngRespuesta, pathRespuesta, "image/png");
                triviaAssetRepository.save(new TriviaAsset(trivia, TipoAsset.RESPUESTA, pathRespuesta, urlRespuesta));
            } catch (Exception e) {
                log.warn("No se pudo renderizar o almacenar imagen para trivia {}: {}", trivia.getId(), e.getMessage());
            }
        }

        // 6. Completar generación
        generation.completar(acumuladasValidas.size());
        log.info("Generación {} completada con éxito: {} trivias guardadas en {} intentos (generadas: {}, descartadas: {})",
                generation.getId(), generation.getCantidadFinal(), generation.getIntentos(),
                generation.getCantidadGenerada(), generation.getCantidadDescartada());

        return generationRepository.save(generation);
    }
}
