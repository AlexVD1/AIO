package com.storyvideo.api.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.domain.*;
import com.storyvideo.api.export.dto.ExportMetadata;
import com.storyvideo.api.export.dto.ExportResult;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.VideoProjectRepository;
import com.storyvideo.api.repository.VideoRenderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final Path exportBasePath;
    private final Path driveBasePath;
    private final StoryRepository storyRepository;
    private final VideoProjectRepository videoProjectRepository;
    private final VideoRenderRepository videoRenderRepository;
    private final AssetStorageService assetStorageService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public ExportService(
            @Value("${story.export.path:./export_videos}") String exportPath,
            @Value("${story.export.drive-path:H:\\Mi unidad\\VideosStories}") String drivePath,
            StoryRepository storyRepository,
            VideoProjectRepository videoProjectRepository,
            VideoRenderRepository videoRenderRepository,
            AssetStorageService assetStorageService,
            ObjectMapper objectMapper) {
        this.exportBasePath = Paths.get(exportPath).toAbsolutePath().normalize();
        this.driveBasePath = (drivePath != null && !drivePath.isBlank()) ? Paths.get(drivePath).toAbsolutePath().normalize() : null;
        this.storyRepository = storyRepository;
        this.videoProjectRepository = videoProjectRepository;
        this.videoRenderRepository = videoRenderRepository;
        this.assetStorageService = assetStorageService;
        this.objectMapper = objectMapper;
        init();
    }

    public ExportService(
            String exportPath,
            StoryRepository storyRepository,
            VideoProjectRepository videoProjectRepository,
            VideoRenderRepository videoRenderRepository,
            AssetStorageService assetStorageService,
            ObjectMapper objectMapper) {
        this(exportPath, null, storyRepository, videoProjectRepository, videoRenderRepository, assetStorageService, objectMapper);
    }

    private void init() {
        try {
            Files.createDirectories(exportBasePath);
            log.info("Directorio de exportación local inicializado en: {}", exportBasePath);
        } catch (IOException e) {
            log.warn("No se pudo crear directorio de exportación en {}: {}", exportBasePath, e.getMessage());
        }

        if (driveBasePath != null) {
            try {
                if (Files.exists(driveBasePath) || Files.exists(driveBasePath.getRoot())) {
                    Files.createDirectories(driveBasePath);
                    log.info("Directorio de sincronización con Google Drive activo en: {}", driveBasePath);
                } else {
                    log.info("Ruta de Google Drive configurada pero unidad no conectada actualmente: {}", driveBasePath);
                }
            } catch (Exception e) {
                log.warn("No se pudo inicializar carpeta Google Drive en {}: {}", driveBasePath, e.getMessage());
            }
        }
    }

    @Transactional
    public ExportResult exportStoryVideo(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("Historia no encontrada para exportar: " + storyId));

        VideoProject project = videoProjectRepository.findByStoryId(storyId)
                .orElseThrow(() -> new IllegalStateException("No existe proyecto de video asociado a la historia: " + storyId));

        List<VideoRender> renders = videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(project.getId());
        VideoRender completedRender = renders.stream()
                .filter(r -> r.getStatus() == VideoRenderStatus.COMPLETED)
                .reduce((first, second) -> second) // obtener el último completado
                .orElseThrow(() -> new IllegalStateException("No existe ningún render completado exitosamente para la historia: " + storyId));

        Path sourceVideoPath = assetStorageService.resolveAbsolutePath(completedRender.getVideoPath());
        if (!Files.exists(sourceVideoPath)) {
            throw new IllegalStateException("El archivo de video físico no existe en: " + sourceVideoPath);
        }

        // Crear nombre seguro de archivo
        String rawTitle = story.getTitle() != null ? story.getTitle() : "historia_" + storyId;
        String safeTitle = rawTitle.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safeTitle.length() > 60) {
            safeTitle = safeTitle.substring(0, 60).trim();
        }

        String videoFileName = String.format("%s.mp4", safeTitle);
        String metadataFileName = String.format("%s_metadata.json", safeTitle);

        Path targetVideoPath = exportBasePath.resolve(videoFileName);
        Path targetMetadataPath = exportBasePath.resolve(metadataFileName);

        try {
            Files.createDirectories(exportBasePath);
            // 1. Copiar video a carpeta exportada
            Files.copy(sourceVideoPath, targetVideoPath, StandardCopyOption.REPLACE_EXISTING);

            // 2. Construir metadata
            List<String> hashtags = generateHashtags(story);
            double duration = project.getTotalDurationSeconds() != null
                    ? project.getTotalDurationSeconds().doubleValue()
                    : 60.0;

            String createdAtStr = story.getCreatedAt() != null
                    ? story.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            boolean hasSubtitles = story.getScenes() != null && !story.getScenes().isEmpty();

            ExportMetadata metadata = new ExportMetadata(
                    story.getId(),
                    story.getTitle(),
                    story.getSynopsis() != null ? story.getSynopsis() : story.getPremise(),
                    hashtags,
                    story.getGenre().name(),
                    "Entertainment",
                    story.getLanguage(),
                    duration,
                    completedRender.getResolution(),
                    "mp4",
                    completedRender.getCodec(),
                    "aac",
                    30,
                    createdAtStr,
                    videoFileName,
                    story.getScenes().size(),
                    hasSubtitles,
                    true,
                    true,
                    "es-MX-JorgeNeural"
            );

            // 3. Escribir metadata.json
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetMetadataPath.toFile(), metadata);

            // 3.1 Sincronización automática directa con Google Drive si la unidad está conectada
            if (driveBasePath != null) {
                try {
                    if (Files.exists(driveBasePath) || Files.exists(driveBasePath.getRoot())) {
                        Files.createDirectories(driveBasePath);
                        Path driveVideoPath = driveBasePath.resolve(videoFileName);
                        Path driveMetadataPath = driveBasePath.resolve(metadataFileName);
                        Files.copy(targetVideoPath, driveVideoPath, StandardCopyOption.REPLACE_EXISTING);
                        Files.copy(targetMetadataPath, driveMetadataPath, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Archivo de video y metadata sincronizados con Google Drive en: {}", driveVideoPath);
                    }
                } catch (Exception e) {
                    log.warn("No se pudo copiar a Google Drive en {}: {}", driveBasePath, e.getMessage());
                }
            }

            // 4. Actualizar estado de la historia
            story.setStatus(StoryStatus.EXPORTED);
            storyRepository.save(story);

            log.info("Historia '{}' exportada exitosamente a: {} con metadata: {}",
                    story.getTitle(), targetVideoPath.getFileName(), targetMetadataPath.getFileName());

            return new ExportResult(targetVideoPath, targetMetadataPath, metadata);

        } catch (IOException e) {
            log.error("Error al exportar video o metadata para historia {}: {}", storyId, e.getMessage());
            throw new RuntimeException("Fallo en exportación de video: " + e.getMessage(), e);
        }
    }

    public ExportMetadata getExportMetadata(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("Historia no encontrada: " + storyId));

        String rawTitle = story.getTitle() != null ? story.getTitle() : "historia_" + storyId;
        String safeTitle = rawTitle.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safeTitle.length() > 60) {
            safeTitle = safeTitle.substring(0, 60).trim();
        }

        Path metadataPath = exportBasePath.resolve(String.format("%s_metadata.json", safeTitle));
        if (Files.exists(metadataPath)) {
            try {
                return objectMapper.readValue(metadataPath.toFile(), ExportMetadata.class);
            } catch (IOException e) {
                log.warn("Error leyendo metadata persistida desde {}: {}", metadataPath, e.getMessage());
            }
        }

        VideoProject project = videoProjectRepository.findByStoryId(storyId).orElse(null);
        double duration = (project != null && project.getTotalDurationSeconds() != null)
                ? project.getTotalDurationSeconds().doubleValue()
                : 60.0;

        return new ExportMetadata(
                story.getId(),
                story.getTitle(),
                story.getSynopsis() != null ? story.getSynopsis() : story.getPremise(),
                generateHashtags(story),
                story.getGenre().name(),
                "Entertainment",
                story.getLanguage(),
                duration,
                "1080x1920",
                "mp4",
                "h264",
                "aac",
                30,
                story.getCreatedAt() != null ? story.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "",
                String.format("%s.mp4", safeTitle),
                story.getScenes() != null ? story.getScenes().size() : 0,
                true,
                true,
                true,
                "es-MX-JorgeNeural"
        );
    }

    public Path getExportedVideoPath(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("Historia no encontrada: " + storyId));

        String rawTitle = story.getTitle() != null ? story.getTitle() : "historia_" + storyId;
        String safeTitle = rawTitle.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safeTitle.length() > 60) {
            safeTitle = safeTitle.substring(0, 60).trim();
        }

        Path target = exportBasePath.resolve(String.format("%s.mp4", safeTitle));
        if (Files.exists(target)) {
            return target;
        }

        VideoProject project = videoProjectRepository.findByStoryId(storyId).orElse(null);
        if (project != null) {
            List<VideoRender> renders = videoRenderRepository.findByVideoProjectIdOrderByAttemptNumberAsc(project.getId());
            for (VideoRender r : renders) {
                if (r.getStatus() == VideoRenderStatus.COMPLETED && r.getVideoPath() != null) {
                    Path p = assetStorageService.resolveAbsolutePath(r.getVideoPath());
                    if (Files.exists(p)) {
                        return p;
                    }
                }
            }
        }

        throw new IllegalStateException("El archivo de video exportado no existe para la historia: " + storyId);
    }

    public Path getExportBasePath() {
        return exportBasePath;
    }

    public List<String> generateHashtags(Story story) {
        List<String> tags = new ArrayList<>();

        // 1. Tags por género
        if (story.getGenre() != null) {
            switch (story.getGenre()) {
                case HORROR -> {
                    tags.add("#horror");
                    tags.add("#terror");
                    tags.add("#creepy");
                    tags.add("#miedo");
                }
                case MYSTERY -> {
                    tags.add("#misterio");
                    tags.add("#suspenso");
                    tags.add("#investigacion");
                }
                case SCI_FI -> {
                    tags.add("#scifi");
                    tags.add("#cienciaficcion");
                    tags.add("#futuro");
                }
                case DYSTOPIA -> {
                    tags.add("#distopia");
                    tags.add("#cyberpunk");
                    tags.add("#apocalipsis");
                }
                case PSYCHOLOGICAL -> {
                    tags.add("#psicologico");
                    tags.add("#mente");
                    tags.add("#thriller");
                }
                case URBAN_LEGEND -> {
                    tags.add("#leyendaurbana");
                    tags.add("#leyendas");
                    tags.add("#sobrenatural");
                }
                default -> {
                    tags.add("#" + story.getGenre().name().toLowerCase().replace("_", ""));
                }
            }
        }

        // 2. Tags temáticos desde el tema de la historia
        if (story.getTheme() != null && !story.getTheme().isBlank()) {
            String cleanTheme = story.getTheme().toLowerCase().replaceAll("[^a-záéíóúñ0-9\\s]", " ");
            for (String word : cleanTheme.split("\\s+")) {
                if (word.length() > 3 && !tags.contains("#" + word)) {
                    tags.add("#" + word);
                }
            }
        }

        // 3. Tags genéricos de storytelling
        tags.add("#storytime");
        tags.add("#historias");
        tags.add("#relatos");
        tags.add("#cuentos");

        // 4. Tags de engagement y retención
        tags.add("#fyp");
        tags.add("#viral");
        tags.add("#parati");

        return tags;
    }
}
