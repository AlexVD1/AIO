package com.kidsanim.api.audio;

import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class AudioCatalogService {

    private static final Logger log = LoggerFactory.getLogger(AudioCatalogService.class);

    private final Path audioBasePath;

    private static final Map<EducationalTopicType, String> TOPIC_BGM_MAP = Map.of(
            EducationalTopicType.COUNTING, "upbeat_counting.mp3",
            EducationalTopicType.COLORS, "playful_colors.mp3",
            EducationalTopicType.SHAPES, "curious_shapes.mp3",
            EducationalTopicType.ALPHABET, "cheerful_alphabet.mp3",
            EducationalTopicType.ANIMALS_AND_SOUNDS, "funny_animals.mp3",
            EducationalTopicType.EMOTIONS, "gentle_emotions.mp3",
            EducationalTopicType.DAILY_ROUTINES, "calm_routine.mp3",
            EducationalTopicType.OPPOSITES, "bouncy_opposites.mp3",
            EducationalTopicType.SIZES, "curious_sizes.mp3",
            EducationalTopicType.NATURE, "peaceful_nature.mp3"
    );

    public AudioCatalogService(KidsVideoProperties videoProperties) {
        String basePath = videoProperties != null && videoProperties.audioLibraryPath() != null
                ? videoProperties.audioLibraryPath()
                : "./audio";
        this.audioBasePath = Paths.get(basePath).toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(audioBasePath.resolve("bgm"));
            Files.createDirectories(audioBasePath.resolve("sfx"));
        } catch (Exception e) {
            log.warn("No se pudieron inicializar directorios del catálogo de audio infantil: {}", e.getMessage());
        }
    }

    public String resolveBgmFileName(EducationalTopicType topicType) {
        if (topicType == null) {
            return "kids_playful_theme.mp3";
        }
        return TOPIC_BGM_MAP.getOrDefault(topicType, "kids_playful_theme.mp3");
    }

    public Path resolveBgmPath(EducationalTopicType topicType) {
        String fileName = resolveBgmFileName(topicType);
        return audioBasePath.resolve("bgm").resolve(fileName);
    }

    public boolean hasBgmFile(EducationalTopicType topicType) {
        Path target = resolveBgmPath(topicType);
        return Files.exists(target) && !Files.isDirectory(target);
    }

    private static final java.util.regex.Pattern VALID_SFX_NAME = java.util.regex.Pattern.compile("^[a-z0-9_]{1,40}$");

    /**
     * Normaliza un nombre de SFX proveniente del LLM. Devuelve null si no es un identificador válido
     * (p. ej. "pauseAfterMs: 2500"), para que se descarte solo ese elemento y no todo el plano.
     */
    public static String normalizeSfxName(String raw) {
        if (raw == null) return null;
        String n = raw.toLowerCase().trim().replace('-', '_').replace(' ', '_');
        if (n.endsWith(".mp3")) n = n.substring(0, n.length() - 4);
        return VALID_SFX_NAME.matcher(n).matches() ? n : null;
    }

    public Path resolveSfxPath(String sfxName) {
        String safeName = (sfxName != null ? sfxName.toLowerCase().trim() : "pop") + ".mp3";
        return audioBasePath.resolve("sfx").resolve(safeName);
    }

    public boolean hasSfxFile(String sfxName) {
        Path target = resolveSfxPath(sfxName);
        return Files.exists(target) && !Files.isDirectory(target);
    }

    public Path getAudioBasePath() {
        return audioBasePath;
    }
}
