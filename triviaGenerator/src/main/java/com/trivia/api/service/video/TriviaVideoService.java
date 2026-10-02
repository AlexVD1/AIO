package com.trivia.api.service.video;

import com.trivia.api.domain.EstadoTrivia;
import com.trivia.api.domain.TipoAsset;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.dto.VideoGenerationRequest;
import com.trivia.api.dto.VideoGenerationResponse;
import com.trivia.api.dto.VideoIntroPreviewRequest;
import com.trivia.api.dto.VideoIntroPreviewResponse;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private final VideoIntroService videoIntroService;

    private final double bgmVolume;
    private final String autoExportPath;

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
            VideoIntroService videoIntroService,
            @Value("${trivia.video.bgm-volume:0.15}") double bgmVolume,
            @Value("${trivia.video.auto-export-path:}") String autoExportPath) {
        this.triviaRepository = triviaRepository;
        this.opcionRepository = opcionRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
        this.rendererService = rendererService;
        this.narrationService = narrationService;
        this.assetExtractor = assetExtractor;
        this.commandBuilder = commandBuilder;
        this.processExecutor = processExecutor;
        this.videoIntroService = videoIntroService;
        this.bgmVolume = bgmVolume;
        this.autoExportPath = autoExportPath;
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
        log.info("Iniciando generación de video {} con {} trivias (formato: {}, introMode: {})",
                videoId, request.triviaIds().size(), request.format(), request.introMode());

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

        // 2. Resolver introducción temática si está configurada
        String resolvedTopic = videoIntroService.resolveTopic(request.introTopic(), trivias);
        String resolvedIntroText = videoIntroService.resolveIntroText(
                request.introMode(),
                request.introTopic(),
                request.introTemplate(),
                request.customIntroText(),
                trivias,
                (trivias.isEmpty() || trivias.get(0).getIdioma() == null) ? "es-MX" : trivias.get(0).getIdioma()
        );

        // 3. Planificar Timeline (con soporte de TTS dinámico e introducción)
        TimelinePlan plan = narrationService.planTimeline(
                trivias,
                opcionesMap,
                request.format(),
                request.withBgm(),
                Boolean.TRUE.equals(request.withTts()),
                request.ttsVoice(),
                resolvedIntroText,
                resolvedTopic
        );
        VideoAssetExtractor.ResolvedAssets staticAssets = assetExtractor.getResolvedAssets();

        // 4. Crear directorio temporal de trabajo para este job
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("trivia_video_" + videoId + "_");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear directorio temporal para video: " + e.getMessage(), e);
        }

        List<String> segmentPaths = new ArrayList<>();

        try {
            // 5. Renderizar escena de introducción (si está habilitada y planificada)
            if (plan.hasIntro()) {
                log.info("Renderizando escena de introducción para video {}: '{}' ({:.1f}s)",
                        videoId, plan.introTiming().introText(), plan.introTiming().duration());

                byte[] introBytes = rendererService.renderIntro(
                        plan.introTiming().topic(),
                        plan.introTiming().introText()
                );

                Path introImgFile = tempDir.resolve("intro.png");
                Files.write(introImgFile, introBytes);

                Path introSegmentFile = tempDir.resolve("intro_segment.mp4");

                List<String> introCmd = commandBuilder.buildIntroSegmentCommand(
                        introImgFile.toAbsolutePath().toString().replace("\\", "/"),
                        plan.introTiming(),
                        plan.format(),
                        staticAssets,
                        introSegmentFile.toAbsolutePath().toString().replace("\\", "/")
                );

                processExecutor.execute(introCmd, tempDir.toFile());
                segmentPaths.add(introSegmentFile.toAbsolutePath().toString().replace("\\", "/"));
            }

            // 6. Renderizar cada segmento individual de trivia
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

            // 8. Título base para redes sociales y nombre de archivo
            String baseTitle = resolveBaseTitle(request.customTitle(), resolvedIntroText, resolvedTopic, trivias);
            String cleanBase = sanitizeFilename(baseTitle);
            String dynamicTitle = baseTitle;
            String dynamicFilename = cleanBase + ".mp4";

            // 9. Auto-exportar a carpeta vigilada (ej. Google Drive) si está configurado
            if (autoExportPath != null && !autoExportPath.isBlank()) {
                try {
                    Path exportDir = Paths.get(autoExportPath);
                    if (!Files.exists(exportDir)) {
                        Files.createDirectories(exportDir);
                    }

                    // Limpieza diaria automática: eliminar videos con más de 24 horas de antigüedad
                    cleanupOldFiles(exportDir, 24);

                    // Numeración secuencial limpia: #1, #2, #3...
                    int seq = 1;
                    while (Files.exists(exportDir.resolve(cleanBase + " #" + seq + ".mp4"))
                            || (seq == 1 && Files.exists(exportDir.resolve(cleanBase + ".mp4")))) {
                        seq++;
                    }

                    dynamicTitle = baseTitle + " #" + seq;
                    dynamicFilename = cleanBase + " #" + seq + ".mp4";

                    Path targetFile = exportDir.resolve(dynamicFilename);
                    Files.copy(finalVideoFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Video exportado automáticamente a carpeta: {}", targetFile.toAbsolutePath());
                } catch (Exception ex) {
                    log.error("No se pudo auto-exportar el video a {}: {}", autoExportPath, ex.getMessage(), ex);
                }
            } else {
                dynamicTitle = baseTitle + " #1";
                dynamicFilename = cleanBase + " #1.mp4";
            }

            // 10. Actualizar estado de las trivias (ACTIVA -> EXPORTADA, DESCARGADA -> DESCARGADA_Y_EXPORTADA)
            for (Trivia t : trivias) {
                if (t.getEstado() == EstadoTrivia.ACTIVA) {
                    t.setEstado(EstadoTrivia.EXPORTADA);
                } else if (t.getEstado() == EstadoTrivia.DESCARGADA) {
                    t.setEstado(EstadoTrivia.DESCARGADA_Y_EXPORTADA);
                }
            }
            triviaRepository.saveAll(trivias);

            log.info("Video {} generado exitosamente: {} ({:.1f}s) - Título: '{}'",
                    videoId, publicUrl, plan.getTotalDuration(), dynamicTitle);

            return new VideoGenerationResponse(
                    videoId,
                    "COMPLETED",
                    publicUrl,
                    trivias.size(),
                    plan.getTotalDuration(),
                    plan.format(),
                    LocalDateTime.now(),
                    dynamicTitle,
                    dynamicFilename
            );

        } catch (IOException e) {
            log.error("Error de E/S durante generación de video {}: {}", videoId, e.getMessage(), e);
            throw new IllegalStateException("Error al generar el video de trivias: " + e.getMessage(), e);
        } finally {
            // 8. Limpiar archivos temporales
            deleteDirectoryQuietly(tempDir.toFile());
        }
    }

    /**
     * Obtiene el catálogo de plantillas disponibles para introducción de video.
     */
    public List<VideoIntroTemplate> getIntroTemplates() {
        return videoIntroService.getCatalog();
    }

    /**
     * Resuelve el tema y texto de introducción para previsualización.
     */
    public VideoIntroPreviewResponse previewIntro(VideoIntroPreviewRequest request) {
        List<Trivia> trivias = new ArrayList<>();
        if (request.triviaIds() != null && !request.triviaIds().isEmpty()) {
            for (UUID id : request.triviaIds()) {
                triviaRepository.findById(id).ifPresent(trivias::add);
            }
        }

        String resolvedTopic = videoIntroService.resolveTopic(request.topic(), trivias);
        String resolvedIntroText = videoIntroService.resolveIntroText(
                request.mode(),
                request.topic(),
                request.template(),
                request.customText(),
                trivias,
                request.idioma()
        );

        return new VideoIntroPreviewResponse(request.mode(), resolvedTopic, resolvedIntroText);
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

    /**
     * Resuelve el título base considerando prioridad:
     * 1. customTitle explícito si fue proporcionado por el usuario.
     * 2. resolvedIntroText (texto de la intro personalizada o plantilla) si existe.
     * 3. Tema formateado como fallback ("Cuanto sabes sobre {tema} - Trivia Challenge").
     */
    public String resolveBaseTitle(String customTitle, String resolvedIntroText, String resolvedTopic, List<Trivia> trivias) {
        if (customTitle != null && !customTitle.isBlank()) {
            return cleanTitleText(customTitle);
        }
        if (resolvedIntroText != null && !resolvedIntroText.isBlank()) {
            return cleanTitleText(resolvedIntroText);
        }
        return generateVideoTitle(resolvedTopic, trivias);
    }

    private String cleanTitleText(String text) {
        if (text == null) return "";
        String clean = text.trim();
        if (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        if (clean.length() > 85) {
            clean = clean.substring(0, 85).trim();
        }
        return clean;
    }

    /**
     * Construye un título dinámico atractivo para redes sociales (YouTube Shorts, TikTok, Facebook Reels).
     */
    public String generateVideoTitle(String resolvedTopic, List<Trivia> trivias) {
        String topic = (resolvedTopic != null && !resolvedTopic.isBlank()) ? resolvedTopic.trim() : "";
        if (topic.isEmpty() && trivias != null && !trivias.isEmpty() && trivias.get(0).getTipoTrivia() != null) {
            topic = trivias.get(0).getTipoTrivia().getNombre();
        }
        if (topic.isEmpty()) {
            topic = "Cultura General";
        }
        // Si el tema incluye aclaraciones largas o paréntesis, recortarlo al núcleo principal
        if (topic.contains("(") || topic.contains(",")) {
            int cutIdx = Math.min(
                    topic.contains("(") ? topic.indexOf("(") : topic.length(),
                    topic.contains(",") ? topic.indexOf(",") : topic.length()
            );
            if (cutIdx > 3) {
                topic = topic.substring(0, cutIdx).trim();
            }
        }
        if (topic.length() > 35) {
            topic = topic.substring(0, 35).trim();
        }
        if (!topic.isEmpty()) {
            topic = Character.toUpperCase(topic.charAt(0)) + topic.substring(1);
        }
        return "Cuanto sabes sobre " + topic + " - Trivia Challenge";
    }

    /**
     * Limpia caracteres inválidos en sistemas de archivos (especialmente Windows) para guardar el video.
     */
    public String sanitizeFilename(String title) {
        if (title == null || title.isBlank()) {
            return "trivia_video";
        }
        // Eliminar caracteres prohibidos en Windows: \ / : * ? " < > |
        // También eliminamos signos ¿ y ¡ para evitar signos huérfanos sin cierre en nombres de archivo
        String clean = title.replaceAll("[\\\\/:*?\"<>|¿¡]", "").trim();
        clean = clean.replaceAll("\\s+", " ");
        if (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        if (clean.length() > 85) {
            clean = clean.substring(0, 85).trim();
        }
        return clean.isEmpty() ? "trivia_video" : clean;
    }

    /**
     * Elimina archivos de video con más de 'maxAgeHours' horas de antigüedad para mantener limpia la carpeta.
     */
    private void cleanupOldFiles(Path directory, int maxAgeHours) {
        try {
            if (directory != null && Files.exists(directory)) {
                long cutoffMillis = System.currentTimeMillis() - (maxAgeHours * 3600 * 1000L);
                try (var stream = Files.list(directory)) {
                    stream.filter(p -> p.toString().toLowerCase().endsWith(".mp4"))
                          .filter(p -> {
                              try {
                                  return Files.getLastModifiedTime(p).toMillis() < cutoffMillis;
                              } catch (IOException e) {
                                  return false;
                              }
                          })
                          .forEach(p -> {
                              try {
                                  Files.deleteIfExists(p);
                                  log.info("Limpieza diaria: video antiguo eliminado de {}: {}", directory, p.getFileName());
                              } catch (IOException e) {
                                  log.warn("No se pudo eliminar archivo antiguo {}: {}", p.getFileName(), e.getMessage());
                              }
                          });
                }
            }
        } catch (Exception e) {
            log.warn("Error en limpieza de archivos antiguos en {}: {}", directory, e.getMessage());
        }
    }
}
