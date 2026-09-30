package com.trivia.api.service;

import com.trivia.api.domain.TipoAsset;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de mantenimiento para re-renderizado masivo de tarjetas gráficas (PNG 1080x1080).
 * Permite regenerar todas las imágenes en disco con la plantilla visual actualizada,
 * eliminando textos antiguos o marcas anteriores de forma limpia y consistente.
 */
@Service
public class TriviaMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(TriviaMaintenanceService.class);

    private final TriviaRepository triviaRepository;
    private final TriviaOpcionRepository opcionRepository;
    private final TriviaAssetRepository assetRepository;
    private final TriviaRendererService rendererService;
    private final AssetStorageService storageService;

    public TriviaMaintenanceService(
            TriviaRepository triviaRepository,
            TriviaOpcionRepository opcionRepository,
            TriviaAssetRepository assetRepository,
            TriviaRendererService rendererService,
            AssetStorageService storageService) {
        this.triviaRepository = triviaRepository;
        this.opcionRepository = opcionRepository;
        this.assetRepository = assetRepository;
        this.rendererService = rendererService;
        this.storageService = storageService;
    }

    public record RerenderResult(
            int totalTrivias,
            int totalImagenesProcesadas,
            int errores,
            String mensaje
    ) {}

    /**
     * Re-renderiza las imágenes de pregunta y respuesta para todas las trivias o una lista específica de IDs.
     * Sobrescribe los archivos PNG en el storage para que muestren el diseño gráfico vigente.
     */
    @Transactional
    public RerenderResult rerenderTrivias(List<UUID> triviaIds) {
        List<Trivia> trivias;
        if (triviaIds != null && !triviaIds.isEmpty()) {
            trivias = triviaRepository.findAllById(triviaIds);
        } else {
            trivias = triviaRepository.findAll();
        }

        log.info("Iniciando re-renderizado masivo de imágenes para {} trivias...", trivias.size());

        int imagenesProcesadas = 0;
        int errores = 0;

        for (Trivia trivia : trivias) {
            try {
                List<TriviaOpcion> opciones = opcionRepository.findByTriviaOrderByLetraAsc(trivia);
                String genId = trivia.getGeneration() != null ? trivia.getGeneration().getId().toString() : "legacy";

                // 1. Re-renderizar tarjeta de PREGUNTA
                byte[] pngPregunta = rendererService.renderPregunta(trivia, opciones);
                Optional<TriviaAsset> assetPreguntaOpt = assetRepository.findByTriviaAndTipo(trivia, TipoAsset.PREGUNTA);
                String pathPregunta;
                if (assetPreguntaOpt.isPresent()) {
                    pathPregunta = assetPreguntaOpt.get().getPath();
                } else {
                    pathPregunta = String.format("%s/%s-pregunta.png", genId, trivia.getId());
                    String urlPregunta = storageService.store(pngPregunta, pathPregunta, "image/png");
                    assetRepository.save(new TriviaAsset(trivia, TipoAsset.PREGUNTA, pathPregunta, urlPregunta));
                }
                storageService.store(pngPregunta, pathPregunta, "image/png");
                imagenesProcesadas++;

                // 2. Re-renderizar tarjeta de RESPUESTA
                byte[] pngRespuesta = rendererService.renderRespuesta(trivia, opciones);
                Optional<TriviaAsset> assetRespuestaOpt = assetRepository.findByTriviaAndTipo(trivia, TipoAsset.RESPUESTA);
                String pathRespuesta;
                if (assetRespuestaOpt.isPresent()) {
                    pathRespuesta = assetRespuestaOpt.get().getPath();
                } else {
                    pathRespuesta = String.format("%s/%s-respuesta.png", genId, trivia.getId());
                    String urlRespuesta = storageService.store(pngRespuesta, pathRespuesta, "image/png");
                    assetRepository.save(new TriviaAsset(trivia, TipoAsset.RESPUESTA, pathRespuesta, urlRespuesta));
                }
                storageService.store(pngRespuesta, pathRespuesta, "image/png");
                imagenesProcesadas++;

            } catch (Exception e) {
                log.error("Error al re-renderizar imágenes de trivia {}: {}", trivia.getId(), e.getMessage());
                errores++;
            }
        }

        String msg = String.format("Re-renderizado completado: %d trivias procesadas, %d imágenes actualizadas limpiamente, %d errores.",
                trivias.size(), imagenesProcesadas, errores);
        log.info(msg);

        return new RerenderResult(trivias.size(), imagenesProcesadas, errores, msg);
    }
}
