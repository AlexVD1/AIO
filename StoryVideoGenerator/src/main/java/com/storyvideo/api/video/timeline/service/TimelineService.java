package com.storyvideo.api.video.timeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.audio.catalog.AudioCatalogService;
import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryScene;
import com.storyvideo.api.domain.TransitionType;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.video.timeline.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TimelineService {

    private static final Logger log = LoggerFactory.getLogger(TimelineService.class);

    private final AudioCatalogService audioCatalogService;
    private final AssetStorageService assetStorageService;
    private final ObjectMapper objectMapper;
    private final double bgmVolume;

    public TimelineService(
            AudioCatalogService audioCatalogService,
            AssetStorageService assetStorageService,
            ObjectMapper objectMapper,
            @Value("${story.video.bgm-volume:0.12}") double bgmVolume) {
        this.audioCatalogService = audioCatalogService;
        this.assetStorageService = assetStorageService;
        this.objectMapper = objectMapper;
        this.bgmVolume = bgmVolume;
    }

    public VideoTimeline buildTimeline(Story story) {
        if (story == null || story.getScenes() == null || story.getScenes().isEmpty()) {
            throw new IllegalArgumentException("No se puede construir un timeline para una historia sin escenas");
        }

        List<StoryScene> sortedScenes = story.getScenes().stream()
                .sorted(Comparator.comparingInt(StoryScene::getSequenceNumber))
                .toList();

        List<TimelineEntry> entries = new ArrayList<>();
        List<VolumeDuckPoint> duckingPoints = new ArrayList<>();
        List<SfxCue> globalSfx = new ArrayList<>();

        double currentTimestamp = 0.0;

        for (int i = 0; i < sortedScenes.size(); i++) {
            StoryScene scene = sortedScenes.get(i);

            // 1. Duración de la narración
            double narrationDuration = scene.getNarrationDurationSeconds() != null
                    ? scene.getNarrationDurationSeconds().doubleValue()
                    : 0.0;

            // 2. Offset inicial para dar respiro al cambio de escena
            double narrationOffset = narrationDuration > 0 ? 0.35 : 0.0;

            // 3. Duración calculada de la escena
            double sceneDuration;
            if (scene.getActualDurationSeconds() != null && scene.getActualDurationSeconds().doubleValue() > 0) {
                sceneDuration = Math.max(scene.getActualDurationSeconds().doubleValue(), narrationDuration + narrationOffset + 0.35);
            } else if (narrationDuration > 0) {
                sceneDuration = narrationDuration + narrationOffset + 0.40;
            } else if (scene.getEstimatedDurationSeconds() != null && scene.getEstimatedDurationSeconds().doubleValue() > 0) {
                sceneDuration = scene.getEstimatedDurationSeconds().doubleValue();
            } else {
                sceneDuration = 5.0; // fallback seguro
            }

            sceneDuration = round(sceneDuration);
            double sceneStartTime = round(currentTimestamp);
            double sceneEndTime = round(sceneStartTime + sceneDuration);

            // 4. Resolver ruta física de la imagen
            String absoluteImagePath = null;
            if (scene.getImagePath() != null && !scene.getImagePath().isBlank()) {
                Path resolved = assetStorageService.resolveAbsolutePath(scene.getImagePath());
                absoluteImagePath = resolved.toString();
            }

            // 5. Resolver ruta física del audio de narración
            String absoluteAudioPath = null;
            if (scene.getNarrationAudioPath() != null && !scene.getNarrationAudioPath().isBlank()) {
                Path resolved = assetStorageService.resolveAbsolutePath(scene.getNarrationAudioPath());
                absoluteAudioPath = resolved.toString();
            }

            // 6. Camera Movement & Transitions
            CameraMovement movement = scene.getCameraMovement() != null ? scene.getCameraMovement() : CameraMovement.SLOW_ZOOM_IN;
            TransitionType transitionIn = scene.getTransitionIn() != null ? scene.getTransitionIn() : TransitionType.CUT;
            TransitionType transitionOut = scene.getTransitionOut() != null ? scene.getTransitionOut() : TransitionType.CUT;

            // 7. Ducking para la narración
            if (absoluteAudioPath != null && narrationDuration > 0) {
                double duckStart = round(sceneStartTime + narrationOffset);
                double duckEnd = round(duckStart + narrationDuration);
                duckingPoints.add(new VolumeDuckPoint(duckStart, duckEnd, 0.05));
            }

            // 8. SFX por escena (ej. whoosh al inicio del hook)
            List<SfxCue> sceneSfx = new ArrayList<>();
            if (scene.getSequenceNumber() == 1 && audioCatalogService != null) {
                Path whooshPath = audioCatalogService.resolveSfxPath("whoosh");
                if (whooshPath != null && Files.exists(whooshPath)) {
                    sceneSfx.add(new SfxCue("whoosh", whooshPath.toString(), 0.0, 0.65));
                }
            }

            TimelineEntry entry = new TimelineEntry(
                    scene.getId(),
                    scene.getSequenceNumber(),
                    sceneStartTime,
                    sceneEndTime,
                    sceneDuration,
                    absoluteImagePath,
                    movement,
                    transitionIn,
                    transitionOut,
                    0.4,
                    List.of(),
                    absoluteAudioPath,
                    narrationOffset,
                    narrationDuration,
                    List.of(),
                    sceneSfx
            );

            entries.add(entry);
            currentTimestamp = sceneEndTime;
        }

        // BGM track resolution
        String musicTrackPath = null;
        if (story.getGenre() != null && audioCatalogService != null) {
            Path bgmPath = audioCatalogService.resolveBgmPath(story.getGenre());
            if (bgmPath != null && Files.exists(bgmPath)) {
                musicTrackPath = bgmPath.toString();
            }
        }

        AudioMixPlan audioMix = new AudioMixPlan(musicTrackPath, bgmVolume, duckingPoints, globalSfx);
        double totalDuration = round(currentTimestamp);

        log.info("Timeline construido para historia '{}': {} escenas, {} segundos",
                story.getTitle(), entries.size(), totalDuration);

        return new VideoTimeline(story.getId(), totalDuration, entries, audioMix);
    }

    public String toJson(VideoTimeline timeline) {
        try {
            return objectMapper.writeValueAsString(timeline);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar VideoTimeline a JSON", e);
        }
    }

    public VideoTimeline fromJson(String json) {
        try {
            return objectMapper.readValue(json, VideoTimeline.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al deserializar JSON a VideoTimeline", e);
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
