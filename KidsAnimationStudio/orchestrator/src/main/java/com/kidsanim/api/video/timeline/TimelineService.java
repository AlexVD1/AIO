package com.kidsanim.api.video.timeline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.audio.AudioCatalogService;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Scene;
import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.SceneRepository;
import com.kidsanim.api.repository.ShotRepository;
import com.kidsanim.api.script.model.OverlaySpec;
import com.kidsanim.api.video.timeline.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class TimelineService {

    private static final Logger log = LoggerFactory.getLogger(TimelineService.class);

    private final SceneRepository sceneRepository;
    private final ShotRepository shotRepository;
    private final AssetRepository assetRepository;
    private final AudioCatalogService audioCatalogService;
    private final KidsVideoProperties videoProperties;
    private final ObjectMapper objectMapper;

    public TimelineService(
            SceneRepository sceneRepository,
            ShotRepository shotRepository,
            AssetRepository assetRepository,
            AudioCatalogService audioCatalogService,
            KidsVideoProperties videoProperties,
            ObjectMapper objectMapper) {
        this.sceneRepository = sceneRepository;
        this.shotRepository = shotRepository;
        this.assetRepository = assetRepository;
        this.audioCatalogService = audioCatalogService;
        this.videoProperties = videoProperties;
        this.objectMapper = objectMapper;
    }

    public VideoTimeline buildTimeline(Episode episode) {
        if (episode == null) {
            throw new IllegalArgumentException("El episodio no puede ser nulo para construir el timeline");
        }

        List<Scene> scenes = episode.getScenes();
        if (scenes == null || scenes.isEmpty()) {
            scenes = sceneRepository.findByEpisodeIdOrderByOrderIndexAsc(episode.getId());
        } else {
            scenes.sort(Comparator.comparingInt(Scene::getOrderIndex));
        }

        if (scenes.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron escenas para el episodio " + episode.getId());
        }

        List<TimelineEntry> entries = new ArrayList<>();
        List<VolumeDuckPoint> duckingPoints = new ArrayList<>();
        List<SfxCue> globalSfx = new ArrayList<>();

        double currentTimestamp = 0.0;
        int globalSequence = 1;

        for (Scene scene : scenes) {
            List<Shot> shots = scene.getShots();
            if (shots == null || shots.isEmpty()) {
                shots = shotRepository.findBySceneIdOrderByOrderIndexAsc(scene.getId());
            } else {
                shots.sort(Comparator.comparingInt(Shot::getOrderIndex));
            }

            for (Shot shot : shots) {
                // 1. Resolver activos (CLIP, KEYFRAME, NARRATION_AUDIO, WORD_TIMESTAMPS)
                String clipPath = resolveAssetPath(shot.getId(), AssetType.CLIP);
                String keyframePath = resolveAssetPath(shot.getId(), AssetType.KEYFRAME);
                String narrationPath = resolveAssetPath(shot.getId(), AssetType.NARRATION_AUDIO);

                // 2. Determinar duración de la narración
                double narrationDuration = 0.0;
                var narrationAssetOpt = assetRepository.findByShotIdAndType(shot.getId(), AssetType.NARRATION_AUDIO);
                if (narrationAssetOpt.isPresent() && narrationAssetOpt.get().getMetaJson() != null) {
                    try {
                        var meta = objectMapper.readTree(narrationAssetOpt.get().getMetaJson());
                        if (meta.has("durationMs")) {
                            narrationDuration = meta.get("durationMs").asDouble() / 1000.0;
                        }
                    } catch (Exception ignored) {
                    }
                }

                // 3. Duración del plano en segundos
                double shotDuration;
                if (shot.getTargetDurationMs() > 0) {
                    shotDuration = shot.getTargetDurationMs() / 1000.0;
                } else if (narrationDuration > 0) {
                    shotDuration = narrationDuration + (shot.getPauseAfterMs() / 1000.0);
                } else {
                    shotDuration = 4.0;
                }
                shotDuration = round(Math.max(2.5, shotDuration));

                double startTime = round(currentTimestamp);
                double endTime = round(startTime + shotDuration);

                // 4. Offset de narración para respiro didáctico (0.25 s)
                double narrationOffset = narrationDuration > 0 ? 0.25 : 0.0;

                // 5. Ducking de BGM durante narración
                if (narrationPath != null && narrationDuration > 0) {
                    double duckStart = round(startTime + narrationOffset);
                    double duckEnd = round(duckStart + narrationDuration);
                    duckingPoints.add(new VolumeDuckPoint(duckStart, duckEnd, 0.05));
                }

                // 6. Overlays parseados del shot
                List<OverlaySpec> overlays = parseOverlays(shot.getOverlaysJson());

                // 7. SFX parseados del shot
                List<SfxCue> shotSfx = resolveSfxCues(shot.getSfxJson(), startTime);

                // Camera motion y continuity
                CameraMotion motion = shot.getCameraMotion() != null ? shot.getCameraMotion() : CameraMotion.STATIC;
                ContinuityMode continuity = shot.getContinuityMode() != null ? shot.getContinuityMode() : ContinuityMode.NEW_KEYFRAME;

                TimelineEntry entry = new TimelineEntry(
                        shot.getId(),
                        globalSequence++,
                        scene.getOrderIndex(),
                        shot.getOrderIndex(),
                        startTime,
                        endTime,
                        shotDuration,
                        clipPath,
                        keyframePath,
                        motion,
                        continuity,
                        shot.getNarrationText(),
                        narrationPath,
                        narrationOffset,
                        narrationDuration,
                        overlays,
                        shotSfx
                );

                entries.add(entry);
                currentTimestamp = endTime;
            }
        }

        // BGM de acuerdo al tema educativo de la serie/episodio
        String musicTrackPath = null;
        if (audioCatalogService != null) {
            Path bgmPath = audioCatalogService.resolveBgmPath(episode.getTopicType());
            if (bgmPath != null && Files.exists(bgmPath) && !Files.isDirectory(bgmPath)) {
                musicTrackPath = bgmPath.toString();
            }
        }

        double totalDuration = round(currentTimestamp);
        double bgmVolume = videoProperties != null && videoProperties.bgmVolume() != null
                ? videoProperties.bgmVolume()
                : 0.12;

        AudioMixPlan audioMix = new AudioMixPlan(musicTrackPath, bgmVolume, duckingPoints, globalSfx);

        int width = videoProperties != null && videoProperties.width() != null ? videoProperties.width() : 1920;
        int height = videoProperties != null && videoProperties.height() != null ? videoProperties.height() : 1080;
        int fps = videoProperties != null && videoProperties.fps() != null ? videoProperties.fps() : 30;

        log.info("Timeline construido para episodio '{}': {} planos, {} segundos, resolución {}x{} a {} fps",
                episode.getTitle(), entries.size(), totalDuration, width, height, fps);

        return new VideoTimeline(episode.getId(), totalDuration, width, height, fps, entries, audioMix);
    }

    private String resolveAssetPath(UUID shotId, AssetType type) {
        var assetOpt = assetRepository.findByShotIdAndType(shotId, type);
        if (assetOpt.isPresent()) {
            String path = assetOpt.get().getPath();
            if (path != null && new File(path).exists()) {
                return path;
            }
            return path; // Retornar ruta lógica para fallback o mocking
        }
        return null;
    }

    private List<OverlaySpec> parseOverlays(String overlaysJson) {
        if (overlaysJson == null || overlaysJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(overlaysJson, new TypeReference<List<OverlaySpec>>() {});
        } catch (Exception e) {
            log.warn("Error al deserializar overlaysJson: {}", e.getMessage());
            return List.of();
        }
    }

    private List<SfxCue> resolveSfxCues(String sfxJson, double shotStartTime) {
        if (sfxJson == null || sfxJson.isBlank() || audioCatalogService == null) {
            return List.of();
        }
        try {
            List<String> sfxNames = objectMapper.readValue(sfxJson, new TypeReference<List<String>>() {});
            List<SfxCue> cues = new ArrayList<>();
            double offset = 0.10;
            for (String rawName : sfxNames) {
                String sfxName = AudioCatalogService.normalizeSfxName(rawName);
                if (sfxName == null) {
                    log.warn("SFX inválido descartado: '{}'", rawName);
                    continue;
                }
                Path path = audioCatalogService.resolveSfxPath(sfxName);
                cues.add(new SfxCue(sfxName, path.toString(), offset, 0.65));
                offset += 0.50;
            }
            return cues;
        } catch (Exception e) {
            log.warn("Error al deserializar sfxJson: {}", e.getMessage());
            return List.of();
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
