package com.kidsanim.api.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.PipelineJob;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.PipelineJobStatus;
import com.kidsanim.api.domain.enums.PipelineStage;
import com.kidsanim.api.export.dto.ExportMetadata;
import com.kidsanim.api.export.dto.ExportResult;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final Path exportBasePath;
    private final EpisodeRepository episodeRepository;
    private final AssetRepository assetRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final KidsVideoProperties videoProperties;
    private final ObjectMapper objectMapper;

    public ExportService(
            EpisodeRepository episodeRepository,
            AssetRepository assetRepository,
            PipelineJobRepository pipelineJobRepository,
            KidsVideoProperties videoProperties,
            ObjectMapper objectMapper) {
        this.episodeRepository = episodeRepository;
        this.assetRepository = assetRepository;
        this.pipelineJobRepository = pipelineJobRepository;
        this.videoProperties = videoProperties;
        this.objectMapper = objectMapper;

        String basePath = (videoProperties != null && videoProperties.exportPath() != null)
                ? videoProperties.exportPath()
                : "./export_videos";
        this.exportBasePath = Paths.get(basePath).toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(exportBasePath);
            log.info("Directorio base de exportación inicializado en: {}", exportBasePath);
        } catch (IOException e) {
            log.warn("No se pudo crear directorio de exportación en {}: {}", exportBasePath, e.getMessage());
        }
    }

    @Transactional
    public ExportResult exportEpisode(UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado para exportar: " + episodeId));

        PipelineJob exportJob = createOrGetJob(episode, PipelineStage.EXPORTING);
        exportJob.setStatus(PipelineJobStatus.RUNNING);
        exportJob.setStartedAt(OffsetDateTime.now());
        exportJob.setProgressPercent(10);
        pipelineJobRepository.save(exportJob);

        episode.setStatus(EpisodeStatus.EXPORTING);
        episodeRepository.save(episode);

        // 1. Obtener ruta del video final
        String sourceVideoStr = episode.getFinalVideoPath();
        if (sourceVideoStr == null || sourceVideoStr.isBlank()) {
            List<Asset> videoAssets = assetRepository.findByEpisodeIdAndType(episodeId, AssetType.FINAL_VIDEO);
            if (!videoAssets.isEmpty()) {
                sourceVideoStr = videoAssets.get(0).getPath();
            }
        }

        if (sourceVideoStr == null || !Files.exists(Paths.get(sourceVideoStr))) {
            String errorMsg = "El archivo de video final no existe en el disco para el episodio: " + episodeId;
            exportJob.setStatus(PipelineJobStatus.FAILED);
            exportJob.setErrorMessage(errorMsg);
            exportJob.setFinishedAt(OffsetDateTime.now());
            pipelineJobRepository.save(exportJob);
            episode.setStatus(EpisodeStatus.FAILED);
            episodeRepository.save(episode);
            throw new IllegalStateException(errorMsg);
        }

        Path sourceVideoPath = Paths.get(sourceVideoStr);

        // 2. Determinar rutas y nombres seguros
        String seriesSlug = slugify(episode.getSeries() != null ? episode.getSeries().getName() : "serie");
        String episodeSlug = slugify(episode.getTitle() != null ? episode.getTitle() : "episodio");
        String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        Path seriesExportDir = exportBasePath.resolve(seriesSlug);
        try {
            Files.createDirectories(seriesExportDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear directorio de exportación de serie: " + seriesExportDir, e);
        }

        Path targetVideoPath = seriesExportDir.resolve(String.format("%s_%s.mp4", datePrefix, episodeSlug));
        Path targetThumbPath = seriesExportDir.resolve(String.format("%s_%s_thumb.png", datePrefix, episodeSlug));
        Path targetMetadataPath = seriesExportDir.resolve(String.format("%s_%s_metadata.json", datePrefix, episodeSlug));

        try {
            // Copiar video a destino
            Files.copy(sourceVideoPath, targetVideoPath, StandardCopyOption.REPLACE_EXISTING);
            exportJob.setProgressPercent(50);
            pipelineJobRepository.save(exportJob);

            // 3. Generar thumbnail a partir de algún keyframe del episodio
            Path sourceKeyframe = findBestKeyframe(episode);
            String exportedThumbStr = null;
            if (sourceKeyframe != null && Files.exists(sourceKeyframe)) {
                Files.copy(sourceKeyframe, targetThumbPath, StandardCopyOption.REPLACE_EXISTING);
                exportedThumbStr = targetThumbPath.toAbsolutePath().toString();
                saveOrUpdateAsset(episode, AssetType.THUMBNAIL, exportedThumbStr, Map.of(
                        "source", sourceKeyframe.toString(),
                        "exportedPath", exportedThumbStr
                ));
            }

            exportJob.setProgressPercent(75);
            pipelineJobRepository.save(exportJob);

            // 4. Generar metadata.json pedagógico y conforme con COPPA / YouTube Kids
            List<String> tags = generateTags(episode);
            double duration = episode.getDurationSeconds() != null ? episode.getDurationSeconds().doubleValue() : 60.0;
            int width = videoProperties != null && videoProperties.width() != null ? videoProperties.width() : 1920;
            int height = videoProperties != null && videoProperties.height() != null ? videoProperties.height() : 1080;

            ExportMetadata metadata = new ExportMetadata(
                    episode.getId(),
                    episode.getTitle(),
                    episode.getSeries() != null ? episode.getSeries().getName() : "Kids Animation Studio",
                    episode.getTopicType() != null ? episode.getTopicType().name() : "GENERAL",
                    episode.getTopicDetail() != null ? episode.getTopicDetail() : "",
                    episode.getLearningObjective() != null ? episode.getLearningObjective() : "",
                    tags,
                    "Education",
                    "es-MX",
                    "2-6",
                    true, // madeForKids
                    true, // containsSyntheticMedia
                    duration,
                    String.format("%dx%d", width, height),
                    targetVideoPath.getFileName().toString(),
                    targetThumbPath.getFileName().toString(),
                    OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetMetadataPath.toFile(), metadata);
            String exportedMetadataStr = targetMetadataPath.toAbsolutePath().toString();
            saveOrUpdateAsset(episode, AssetType.METADATA, exportedMetadataStr, Map.of(
                    "exportedPath", exportedMetadataStr
            ));

            // Finalizar pipeline del episodio exitosamente
            exportJob.setStatus(PipelineJobStatus.COMPLETED);
            exportJob.setProgressPercent(100);
            exportJob.setFinishedAt(OffsetDateTime.now());
            pipelineJobRepository.save(exportJob);

            episode.setStatus(EpisodeStatus.COMPLETED);
            episodeRepository.save(episode);

            log.info("Episodio '{}' ({}) exportado exitosamente a {}",
                    episode.getTitle(), episode.getId(), targetVideoPath.toAbsolutePath());

            return new ExportResult(
                    episode.getId(),
                    targetVideoPath.toAbsolutePath().toString(),
                    exportedThumbStr,
                    exportedMetadataStr,
                    true,
                    "Episodio exportado correctamente"
            );

        } catch (Exception e) {
            log.error("Fallo al exportar episodio {}: {}", episode.getId(), e.getMessage(), e);
            exportJob.setStatus(PipelineJobStatus.FAILED);
            exportJob.setErrorMessage(e.getMessage());
            exportJob.setFinishedAt(OffsetDateTime.now());
            pipelineJobRepository.save(exportJob);
            episode.setStatus(EpisodeStatus.FAILED);
            episodeRepository.save(episode);
            throw new RuntimeException("Error durante la exportación del episodio: " + e.getMessage(), e);
        }
    }

    private Path findBestKeyframe(Episode episode) {
        // Buscar keyframe de algún plano del episodio
        List<Asset> episodeAssets = assetRepository.findByEpisodeId(episode.getId());
        for (Asset a : episodeAssets) {
            if (a.getType() == AssetType.KEYFRAME && a.getPath() != null) {
                Path p = Paths.get(a.getPath());
                if (Files.exists(p)) {
                    return p;
                }
            }
        }
        return null;
    }

    private List<String> generateTags(Episode episode) {
        List<String> tags = new ArrayList<>();
        tags.add("animacion infantil");
        tags.add("educacion preescolar");
        tags.add("aprender jugando");
        tags.add("videos para ninos");

        if (episode.getSeries() != null && episode.getSeries().getName() != null) {
            tags.add(episode.getSeries().getName().toLowerCase());
        }

        if (episode.getTopicType() != null) {
            tags.add(episode.getTopicType().name().toLowerCase());
        }

        return tags;
    }

    private String slugify(String text) {
        if (text == null || text.isBlank()) return "item";
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD);
        String slug = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }

    private PipelineJob createOrGetJob(Episode episode, PipelineStage stage) {
        return pipelineJobRepository.findByEpisodeIdAndStage(episode.getId(), stage)
                .orElseGet(() -> {
                    PipelineJob newJob = new PipelineJob(episode, stage);
                    return pipelineJobRepository.save(newJob);
                });
    }

    private void saveOrUpdateAsset(Episode episode, AssetType type, String path, Map<String, Object> metadata) {
        String metaJson = null;
        try {
            if (metadata != null) {
                metaJson = objectMapper.writeValueAsString(metadata);
            }
        } catch (Exception ignored) {}

        List<Asset> existingAssets = assetRepository.findByEpisodeIdAndType(episode.getId(), type);
        Asset asset;
        if (existingAssets != null && !existingAssets.isEmpty()) {
            asset = existingAssets.get(0);
            asset.setPath(path);
            asset.setMetaJson(metaJson);
        } else {
            asset = new Asset(episode, type, path);
            asset.setMetaJson(metaJson);
        }
        assetRepository.save(asset);
    }

    public Path getExportBasePath() {
        return exportBasePath;
    }
}
