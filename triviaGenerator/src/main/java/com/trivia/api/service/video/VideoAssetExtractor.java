package com.trivia.api.service.video;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Componente responsable de garantizar que los assets estáticos (efectos de sonido, música y fuentes)
 * estén extraídos del classpath hacia el filesystem local para que el binario externo de FFmpeg
 * pueda leerlos directamente.
 */
@Component
public class VideoAssetExtractor {

    private static final Logger log = LoggerFactory.getLogger(VideoAssetExtractor.class);

    private final ResourceLoader resourceLoader;
    private final String targetBaseDir;

    private Path tickAudioPath;
    private Path correctAudioPath;
    private Path whooshAudioPath;
    private Path bgmAudioPath;
    private Path fontPath;

    public record ResolvedAssets(
            String tickAudioPath,
            String correctAudioPath,
            String whooshAudioPath,
            String bgmAudioPath,
            String fontPath
    ) {}

    public VideoAssetExtractor(
            ResourceLoader resourceLoader,
            @Value("${trivia.storage.path:./storage}") String storageBasePath) {
        this.resourceLoader = resourceLoader;
        this.targetBaseDir = storageBasePath;
    }

    @PostConstruct
    public void initAssets() {
        try {
            Path assetsDir = Paths.get(targetBaseDir, ".video-assets").toAbsolutePath().normalize();
            Files.createDirectories(assetsDir);

            this.tickAudioPath = extractResource("classpath:video-assets/audio/tick.wav", assetsDir.resolve("tick.wav"));
            this.correctAudioPath = extractResource("classpath:video-assets/audio/correct.wav", assetsDir.resolve("correct.wav"));
            this.whooshAudioPath = extractResource("classpath:video-assets/audio/whoosh.wav", assetsDir.resolve("whoosh.wav"));
            this.bgmAudioPath = extractResource("classpath:video-assets/audio/bgm_loop.wav", assetsDir.resolve("bgm_loop.wav"));
            this.fontPath = extractResource("classpath:video-assets/fonts/font.ttf", assetsDir.resolve("font.ttf"));

            log.info("Assets de video inicializados exitosamente en: {}", assetsDir);
        } catch (IOException e) {
            log.error("Error al inicializar assets de video en filesystem: {}", e.getMessage(), e);
            throw new IllegalStateException("No se pudieron inicializar los assets para generación de video", e);
        }
    }

    private Path extractResource(String resourceLocation, Path destination) throws IOException {
        Resource resource = resourceLoader.getResource(resourceLocation);
        if (!resource.exists()) {
            log.warn("El recurso {} no existe en el classpath", resourceLocation);
            return destination;
        }

        if (!Files.exists(destination) || Files.size(destination) == 0) {
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Recurso extraído: {} -> {}", resourceLocation, destination);
            }
        }
        return destination;
    }

    public ResolvedAssets getResolvedAssets() {
        return new ResolvedAssets(
                formatPath(tickAudioPath),
                formatPath(correctAudioPath),
                formatPath(whooshAudioPath),
                formatPath(bgmAudioPath),
                formatPath(fontPath)
        );
    }

    private String formatPath(Path path) {
        if (path == null) return "";
        return path.toAbsolutePath().normalize().toString().replace("\\", "/");
    }
}
