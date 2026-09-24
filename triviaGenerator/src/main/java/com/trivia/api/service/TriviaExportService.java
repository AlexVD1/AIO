package com.trivia.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trivia.api.domain.TipoAsset;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.dto.TriviaResponse;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servicio encargado de exportar trivias individuales o múltiples en un archivo ZIP estructurado.
 *
 * El archivo ZIP resultante contiene:
 *   - trivias.json: metadatos y opciones estructuradas de todas las trivias seleccionadas.
 *   - trivia_{id}.json: metadatos individuales para facilitar procesamiento granular.
 *   - trivia_{id}_question.png: imagen de 1080x1080 de la pregunta (sin respuesta destacada).
 *   - trivia_{id}_answer.png: imagen de 1080x1080 con la respuesta destacada.
 *
 * Estructura plana (sin subdirectorios individuales) y nombres con ID único para evitar colisiones.
 */
@Service
@Transactional(readOnly = true)
public class TriviaExportService {

    private static final Logger log = LoggerFactory.getLogger(TriviaExportService.class);

    private final TriviaRepository triviaRepository;
    private final TriviaOpcionRepository opcionRepository;
    private final TriviaAssetRepository assetRepository;
    private final AssetStorageService storageService;
    private final TriviaRendererService rendererService;
    private final ObjectMapper objectMapper;

    public TriviaExportService(
            TriviaRepository triviaRepository,
            TriviaOpcionRepository opcionRepository,
            TriviaAssetRepository assetRepository,
            AssetStorageService storageService,
            TriviaRendererService rendererService,
            ObjectMapper objectMapper) {
        this.triviaRepository = triviaRepository;
        this.opcionRepository = opcionRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
        this.rendererService = rendererService;
        this.objectMapper = objectMapper;
    }

    /**
     * Empaqueta las trivias especificadas en un único stream ZIP.
     *
     * @param triviaIds Lista de identificadores UUID de trivias
     * @param os OutputStream destino para la escritura del archivo comprimido
     * @throws IOException Si ocurre un error de E/S al escribir en el stream
     */
    public void exportarZip(List<UUID> triviaIds, OutputStream os) throws IOException {
        if (triviaIds == null || triviaIds.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un ID de trivia para exportar");
        }

        List<UUID> uniqueIds = triviaIds.stream().filter(Objects::nonNull).distinct().toList();
        if (uniqueIds.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un ID válido de trivia para exportar");
        }

        List<Trivia> trivias = triviaRepository.findAllById(uniqueIds);
        if (trivias.isEmpty()) {
            throw new NoSuchElementException("No se encontraron trivias para los IDs proporcionados");
        }

        List<TriviaResponse> responses = new ArrayList<>();
        Map<Trivia, List<TriviaOpcion>> opcionesMap = new HashMap<>();
        Map<Trivia, List<TriviaAsset>> assetsMap = new HashMap<>();

        for (Trivia trivia : trivias) {
            List<TriviaOpcion> opciones = opcionRepository.findByTriviaOrderByLetraAsc(trivia);
            List<TriviaAsset> assets = assetRepository.findByTrivia(trivia);
            opcionesMap.put(trivia, opciones);
            assetsMap.put(trivia, assets);
            responses.add(TriviaResponse.from(trivia, opciones, assets));
        }

        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            // 1. Archivo consolidado trivias.json
            ZipEntry jsonEntry = new ZipEntry("trivias.json");
            zos.putNextEntry(jsonEntry);
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(responses)
                    .getBytes(StandardCharsets.UTF_8);
            zos.write(jsonBytes);
            zos.closeEntry();

            // 2. Archivo individual JSON por trivia ({id}.json)
            for (TriviaResponse dto : responses) {
                ZipEntry singleJsonEntry = new ZipEntry(String.format("%s.json", dto.id()));
                zos.putNextEntry(singleJsonEntry);
                byte[] singleJsonBytes = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(dto)
                        .getBytes(StandardCharsets.UTF_8);
                zos.write(singleJsonBytes);
                zos.closeEntry();
            }

            // 3. Imágenes question.png y answer.png con ID único y estructura plana
            for (Trivia trivia : trivias) {
                List<TriviaAsset> assets = assetsMap.get(trivia);
                List<TriviaOpcion> opciones = opcionesMap.get(trivia);

                // {id}_question.png
                byte[] questionBytes = getAssetBytes(trivia, assets, TipoAsset.PREGUNTA, opciones, true);
                if (questionBytes != null && questionBytes.length > 0) {
                    ZipEntry qEntry = new ZipEntry(String.format("%s_question.png", trivia.getId()));
                    zos.putNextEntry(qEntry);
                    zos.write(questionBytes);
                    zos.closeEntry();
                }

                // {id}_answer.png
                byte[] answerBytes = getAssetBytes(trivia, assets, TipoAsset.RESPUESTA, opciones, false);
                if (answerBytes != null && answerBytes.length > 0) {
                    ZipEntry aEntry = new ZipEntry(String.format("%s_answer.png", trivia.getId()));
                    zos.putNextEntry(aEntry);
                    zos.write(answerBytes);
                    zos.closeEntry();
                }
            }

            zos.finish();
        }
    }

    private byte[] getAssetBytes(Trivia trivia, List<TriviaAsset> assets, TipoAsset tipo, List<TriviaOpcion> opciones, boolean isQuestion) {
        if (assets != null) {
            Optional<TriviaAsset> assetOpt = assets.stream().filter(a -> a.getTipo() == tipo).findFirst();
            if (assetOpt.isPresent()) {
                String path = assetOpt.get().getPath();
                if (path != null && storageService.exists(path)) {
                    try {
                        return storageService.retrieve(path);
                    } catch (Exception e) {
                        log.warn("Error al leer asset desde storage ({}) para trivia {}: {}", path, trivia.getId(), e.getMessage());
                    }
                }
            }
        }

        // Fallback: renderizado en memoria si no está en storage
        try {
            if (isQuestion) {
                return rendererService.renderPregunta(trivia, opciones);
            } else {
                return rendererService.renderRespuesta(trivia, opciones);
            }
        } catch (Exception e) {
            log.error("No se pudo generar imagen de respaldo para trivia {}: {}", trivia.getId(), e.getMessage());
            return null;
        }
    }
}
