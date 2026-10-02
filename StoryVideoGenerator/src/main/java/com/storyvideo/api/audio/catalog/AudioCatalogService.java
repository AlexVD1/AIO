package com.storyvideo.api.audio.catalog;

import com.storyvideo.api.domain.StoryGenre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class AudioCatalogService {

    private static final Logger log = LoggerFactory.getLogger(AudioCatalogService.class);

    private final Path audioBasePath;

    public record AudioTrack(String name, String relativePath, double durationSeconds, String genre) {}

    private static final Map<StoryGenre, String> DEFAULT_BGM_MAP = Map.of(
            StoryGenre.HORROR, "horror_dark_ambient.mp3",
            StoryGenre.MYSTERY, "mystery_noir_suspense.mp3",
            StoryGenre.SCI_FI, "scifi_synth_drone.mp3",
            StoryGenre.DYSTOPIA, "scifi_synth_drone.mp3",
            StoryGenre.PSYCHOLOGICAL, "psychological_heartbeat_tension.mp3",
            StoryGenre.URBAN_LEGEND, "mystery_noir_suspense.mp3",
            StoryGenre.HYPOTHETICAL, "psychological_heartbeat_tension.mp3",
            StoryGenre.STRANGE_EVENTS, "horror_dark_ambient.mp3",
            StoryGenre.EPISODIC, "mystery_noir_suspense.mp3"
    );

    public AudioCatalogService(@Value("${story.audio.catalog-path:./audio}") String catalogPath) {
        this.audioBasePath = Paths.get(catalogPath).toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(audioBasePath.resolve("bgm"));
            Files.createDirectories(audioBasePath.resolve("sfx"));
        } catch (Exception e) {
            log.warn("No se pudo inicializar directorios del catálogo de audio: {}", e.getMessage());
        }
    }

    public String resolveBgmFileName(StoryGenre genre) {
        return DEFAULT_BGM_MAP.getOrDefault(genre, "horror_dark_ambient.mp3");
    }

    public Path resolveBgmPath(StoryGenre genre) {
        String fileName = resolveBgmFileName(genre);
        Path target = audioBasePath.resolve("bgm").resolve(fileName);
        if (Files.exists(target)) {
            return target;
        }

        // Si no existe archivo físico en disco, retornar la ruta teórica
        return target;
    }

    public Path resolveSfxPath(String sfxType) {
        String safeName = (sfxType != null ? sfxType.toLowerCase().trim() : "whoosh") + ".mp3";
        return audioBasePath.resolve("sfx").resolve(safeName);
    }

    public boolean hasBgmFile(StoryGenre genre) {
        return Files.exists(resolveBgmPath(genre));
    }
}
