package com.trivia.api.service.video;

import com.trivia.api.domain.EstadoTrivia;
import com.trivia.api.domain.TipoAsset;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.dto.VideoGenerationRequest;
import com.trivia.api.dto.VideoGenerationResponse;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import com.trivia.api.service.AssetStorageService;
import com.trivia.api.service.TriviaRendererService;
import com.trivia.api.service.video.narration.NarrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio orquestador del pipeline de generación de video para trivias.
 */
@Service
public class TriviaVideoService {

    private static final Logger log = LoggerFactory.getLogger(TriviaVideoService.class);

    private final TriviaRepository triviaRepository;
    private final TriviaOpcionRepository opcionRepository;
    private final TriviaAssetRepository assetRepository;
    private final AssetStorageService storageService;
    private final TriviaRendererService rendererService;
    private final NarrationService narrationService;
    private final VideoAssetExtractor assetExtractor;
    private final FFmpegCommandBuilder commandBuilder;
    private final FFmpegProcessExecutor processExecutor;

    private final double bgmVolume;

    public TriviaVideoService(
            TriviaRepository triviaRepository,
            TriviaOpcionRepository opcionRepository,
            TriviaAssetRepository assetRepository,
            AssetStorageService storageService,
            TriviaRendererService rendererService,
            NarrationService narrationService,
            VideoAssetExtractor assetExtractor,
            FFmpegCommandBuilder commandBuilder,
            FFmpegProcessExecutor processExecutor,
            @Value("${trivia.video.bgm-volume:0.15}") double bgmVolume) {
        this.triviaRepository = triviaRepository;
        this.opcionRepository = opcionRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
        this.rendererService = rendererService;
        this.narrationService = narrationService;
        this.assetExtractor = assetExtractor;
        this.commandBuilder = commandBuilder;
        this.processExecutor = processExecutor;
        this.bgmVolume = bgmVolume;
    }

    /**
     * Genera un video completo reuniendo las trivias solicitadas en el orden especificado.
     */
    @Transactional
    public VideoGenerationResponse generarVideo(VideoGenerationRequest request) {
        if (request.triviaIds() == null || request.triviaIds().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un ID de trivia para generar el video");
        }

        UUID videoId = UUID.randomUUID();
        log.info("Iniciando generación de video {} con {} trivias (formato: {})",
                videoId, request.triviaIds().size(), request.format());

        // 1. Cargar trivias preservando el orden exacto de la petición
        List<Trivia> trivias = new ArrayList<>();
        Map<Trivia, List<TriviaOpcion>> opcionesMap = new HashMap<>();
        Map<Trivia, List<TriviaAsset>> assetsMap = new HashMap<>();

        for (UUID id : request.triviaIds()) {
            Trivia trivia = triviaRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("No se encontró la trivia con ID: " + id));
            trivias.add(trivia);
            opcionesMap.put(trivia, opcionRepository.findByTriviaOrderByLetraAsc(trivia));
            assetsMap.put(trivia, assetRepository.findByTrivia(trivia));
        }

        // 2. Planificar Timeline (con soporte de TTS dinámico si se solicita)
        TimelinePlan plan = narrationService.planTimeline(
                trivias,
                opcionesMap,
                request.format(),
                request.withBgm(),
                Boolean.TRUE.equals(request.withTts()),
                request.ttsVoice()
        );
        VideoAssetExtractor.ResolvedAssets staticAssets = assetExtractor.getResolvedAssets();

        // 3. Crear directorio temporal de trabajo para este job
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("trivia_video_" + videoId + "_");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear directorio temporal para video: " + e.getMessage(), e);
        }

        List<String> segmentPaths = new ArrayList<>();

        try {
            // 4. Renderizar cada segmento individual de trivia
            for (int i = 0; i < trivias.size(); i++) {
                Trivia trivia = trivias.get(i);
                TriviaSceneTiming timing = plan.scenes().get(i);

                log.debug("Procesando segmento {}/{}: trivia {}", i + 1, trivias.size(), trivia.getId());

                // Obtener imágenes PNG (desde storage o fallback en memoria)
                byte[] qBytes = getAssetBytes(trivia, assetsMap.get(trivia), TipoAsset.PREGUNTA, opcionesMap.get(trivia), true);
                byte[] aBytes = getAssetBytes(trivia, assetsMap.get(trivia), TipoAsset.RESPUESTA, opcionesMap.get(trivia), false);

                Path qImgFile = tempDir.resolve(String.format("q_%02d.png", i));
                Path aImgFile = tempDir.resolve(String.format("a_%02d.png", i));
                Files.write(qImgFile, qBytes);
                Files.write(aImgFile, aBytes);

                Path segmentFile = tempDir.resolve(String.format("segment_%02d.mp4", i));

                List<String> segCmd = commandBuilder.buildSegmentCommand(
                        qImgFile.toAbsolutePath().toString().replace("\\", "/"),
                        aImgFile.toAbsolutePath().toString().replace("\\", "/"),
                        timing,
                        plan.format(),
                        staticAssets,
                        segmentFile.toAbsolutePath().toString().replace("\\", "/")
                );

                processExecutor.execute(segCmd, tempDir.toFile());
                segmentPaths.add(segmentFile.toAbsolutePath().toString().replace("\\", "/"));
            }

            // 5. Concatenar todos los segmentos
            Path concatListFile = tempDir.resolve("concat_list.txt");
            StringBuilder sb = new StringBuilder();
            for (String sp : segmentPaths) {
                sb.append("file '").append(sp).append("'\n");
            }
            Files.writeString(concatListFile, sb.toString(), StandardCharsets.UTF_8);

            Path rawConcatFile = tempDir.resolve("raw_concat.mp4");
            List<String> concatCmd = commandBuilder.buildConcatCommand(
                    concatListFile.toAbsolutePath().toString().replace("\\", "/"),
                    rawConcatFile.toAbsolutePath().toString().replace("\\", "/")
            );
            processExecutor.execute(concatCmd, tempDir.toFile());

            // 6. Mezclar música de fondo si se solicitó (volumen atenuado si hay voz TTS)
            Path finalVideoFile = tempDir.resolve("final_video.mp4");
            double effectiveBgmVolume = Boolean.TRUE.equals(request.withTts()) ? 0.09 : bgmVolume;

            if (plan.withBgm() && staticAssets.bgmAudioPath() != null && !staticAssets.bgmAudioPath().isBlank()) {
                List<String> bgmCmd = commandBuilder.buildBgmCommand(
                        rawConcatFile.toAbsolutePath().toString().replace("\\", "/"),
                        staticAssets.bgmAudioPath(),
                        plan.getTotalDuration(),
                        effectiveBgmVolume,
                        finalVideoFile.toAbsolutePath().toString().replace("\\", "/")
                );
                processExecutor.execute(bgmCmd, tempDir.toFile());
            } else {
                Files.copy(rawConcatFile, finalVideoFile);
            }

            // 7. Persistir en el almacenamiento permanente de assets
            byte[] finalVideoBytes = Files.readAllBytes(finalVideoFile);
            String relativeStoragePath = "videos/" + videoId + ".mp4";
            String publicUrl = storageService.store(finalVideoBytes, relativeStoragePath, "video/mp4");

            // 8. Actualizar estado de las trivias (ACTIVA -> EXPORTADA, DESCARGADA -> DESCARGADA_Y_EXPORTADA)
            for (Trivia t : trivias) {
                if (t.getEstado() == EstadoTrivia.ACTIVA) {
                    t.setEstado(EstadoTrivia.EXPORTADA);
                } else if (t.getEstado() == EstadoTrivia.DESCARGADA) {
                    t.setEstado(EstadoTrivia.DESCARGADA_Y_EXPORTADA);
                }
            }
            triviaRepository.saveAll(trivias);

            log.info("Video {} generado exitosamente: {} ({:.1f}s)",
                    videoId, publicUrl, plan.getTotalDuration());

            return new VideoGenerationResponse(
                    videoId,
                    "COMPLETED",
                    publicUrl,
                    trivias.size(),
                    plan.getTotalDuration(),
                    plan.format(),
                    LocalDateTime.now()
            );

        } catch (IOException e) {
            log.error("Error de E/S durante generación de video {}: {}", videoId, e.getMessage(), e);
            throw new IllegalStateException("Error al generar el video de trivias: " + e.getMessage(), e);
        } finally {
            // 8. Limpiar archivos temporales
            deleteDirectoryQuietly(tempDir.toFile());
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
            throw new IllegalStateException("No se pudo obtener imagen para trivia " + trivia.getId(), e);
        }
    }

    private void deleteDirectoryQuietly(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectoryQuietly(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}
