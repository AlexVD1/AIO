package com.storyvideo.api.service;

import com.storyvideo.api.audio.narration.NarrationService;
import com.storyvideo.api.audio.narration.NoOpNarrationService;
import com.storyvideo.api.audio.narration.dto.NarrationRequest;
import com.storyvideo.api.audio.narration.dto.NarrationResult;
import com.storyvideo.api.domain.*;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.dto.StorySceneResponseDto;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.GeneratedAssetRepository;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.StorySceneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class StoryAudioService {

    private static final Logger log = LoggerFactory.getLogger(StoryAudioService.class);
    private static final double CINEMATIC_PAUSE_SECONDS = 0.75; // Pausa cinematográfica al final de cada escena

    private final NarrationService primaryNarrationService;
    private final NoOpNarrationService noOpNarrationService;
    private final StoryRepository storyRepository;
    private final StorySceneRepository sceneRepository;
    private final GeneratedAssetRepository assetRepository;
    private final AssetStorageService storageService;

    public StoryAudioService(
            NarrationService primaryNarrationService,
            @Qualifier("noOpNarrationService") NoOpNarrationService noOpNarrationService,
            StoryRepository storyRepository,
            StorySceneRepository sceneRepository,
            GeneratedAssetRepository assetRepository,
            AssetStorageService storageService) {
        this.primaryNarrationService = primaryNarrationService;
        this.noOpNarrationService = noOpNarrationService;
        this.storyRepository = storyRepository;
        this.sceneRepository = sceneRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
    }

    @Transactional
    public StoryResponseDto generateAudioForStory(UUID storyId, String voiceOverride) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NoSuchElementException("Historia no encontrada con ID: " + storyId));

        log.info("Iniciando generación de locución para historia '{}' (ID: {})", story.getTitle(), storyId);

        String defaultVoice = resolveVoiceForStory(story, voiceOverride);
        List<StoryScene> scenes = sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId);

        for (StoryScene scene : scenes) {
            synthesizeAndStoreSceneAudio(story, scene, defaultVoice);
        }

        // Recalcular el timeline completo con las duraciones reales
        recalculateStoryTimeline(story, scenes);

        storyRepository.save(story);
        return StoryResponseDto.fromEntity(story, storageService);
    }

    @Transactional
    public StorySceneResponseDto regenerateSceneAudio(UUID storyId, UUID sceneId, String voiceOverride) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NoSuchElementException("Historia no encontrada con ID: " + storyId));

        StoryScene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new NoSuchElementException("Escena no encontrada con ID: " + sceneId));

        if (scene.getStory() != null && scene.getStory().getId() != null && !scene.getStory().getId().equals(storyId)) {
            throw new IllegalArgumentException("La escena " + sceneId + " no pertenece a la historia " + storyId);
        }

        String voice = resolveVoiceForStory(story, voiceOverride);
        synthesizeAndStoreSceneAudio(story, scene, voice);

        // Recalcular timeline de la historia
        List<StoryScene> allScenes = sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId);
        recalculateStoryTimeline(story, allScenes);

        storyRepository.save(story);
        return StorySceneResponseDto.fromEntity(scene, storageService);
    }

    private void synthesizeAndStoreSceneAudio(Story story, StoryScene scene, String voice) {
        String textToSpeak = scene.getNarrationScript() != null && !scene.getNarrationScript().isBlank()
                ? scene.getNarrationScript()
                : scene.getNarrationText();

        NarrationRequest request = new NarrationRequest(
                story.getId(),
                scene.getId(),
                scene.getSequenceNumber(),
                textToSpeak,
                voice,
                scene.getNarrationEmotion(),
                "+0%",
                "+0Hz"
        );

        NarrationResult result;
        try {
            if (primaryNarrationService.isAvailable()) {
                result = primaryNarrationService.synthesize(request);
            } else {
                log.warn("Servicio primario de narración ({}) no disponible. Usando fallback No-Op.", primaryNarrationService.getProviderName());
                result = noOpNarrationService.synthesize(request);
            }
        } catch (Exception e) {
            log.error("Fallo durante síntesis de voz con {}: {}. Usando fallback No-Op.", primaryNarrationService.getProviderName(), e.getMessage());
            result = noOpNarrationService.synthesize(request);
        }

        // Guardar archivo físico en almacenamiento
        String relativePath = String.format("stories/%s/audio/narration/scene_%02d.mp3",
                story.getId(), scene.getSequenceNumber());

        storageService.store(relativePath, result.audioBytes());

        // Registrar GeneratedAsset
        GeneratedAsset asset = new GeneratedAsset(
                scene,
                AssetType.NARRATION_AUDIO,
                relativePath,
                result.audioFormat(),
                result.audioBytes().length,
                scene.getVersion()
        );
        assetRepository.save(asset);

        // Actualizar datos de escena
        BigDecimal narrationDuration = BigDecimal.valueOf(result.durationSeconds()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualDuration = BigDecimal.valueOf(result.durationSeconds() + CINEMATIC_PAUSE_SECONDS).setScale(2, RoundingMode.HALF_UP);

        scene.setNarrationAudioPath(relativePath);
        scene.setNarrationDurationSeconds(narrationDuration);
        scene.setActualDurationSeconds(actualDuration);

        // Si la imagen ya fue generada, la escena pasa a READY; si no, AUDIO_READY
        if (scene.getImagePath() != null && !scene.getImagePath().isBlank()) {
            scene.setStatus(SceneStatus.READY);
        } else {
            scene.setStatus(SceneStatus.AUDIO_READY);
        }

        sceneRepository.save(scene);
        log.info("Audio de escena {} procesado ({} s) -> {}", scene.getSequenceNumber(), narrationDuration, relativePath);
    }

    private void recalculateStoryTimeline(Story story, List<StoryScene> scenes) {
        BigDecimal currentOffset = BigDecimal.ZERO;

        for (StoryScene s : scenes) {
            s.setStartTimeSeconds(currentOffset);
            BigDecimal duration = s.getActualDurationSeconds() != null && s.getActualDurationSeconds().compareTo(BigDecimal.ZERO) > 0
                    ? s.getActualDurationSeconds()
                    : s.getEstimatedDurationSeconds();

            currentOffset = currentOffset.add(duration);
            s.setEndTimeSeconds(currentOffset);
            sceneRepository.save(s);
        }

        int totalSeconds = currentOffset.setScale(0, RoundingMode.CEILING).intValue();
        story.setTargetDurationSeconds(totalSeconds);
        log.info("Timeline recalculado para historia '{}': {} segundos totales ({} escenas)",
                story.getTitle(), totalSeconds, scenes.size());
    }

    private String resolveVoiceForStory(Story story, String override) {
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        // Voces por defecto según idioma
        if (story.getLanguage() != null && story.getLanguage().startsWith("es")) {
            return "es-MX-JorgeNeural"; // Voz masculina profunda, ideal para misterio/horror
        }
        return "en-US-ChristopherNeural";
    }
}
