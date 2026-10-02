package com.storyvideo.api.service;

import com.storyvideo.api.domain.*;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.dto.StorySceneResponseDto;
import com.storyvideo.api.image.ImageGenerationService;
import com.storyvideo.api.image.PlaceholderImageService;
import com.storyvideo.api.image.dto.GeneratedImage;
import com.storyvideo.api.image.dto.ImageGenerationRequest;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.GeneratedAssetRepository;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.StorySceneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

@Service
public class StoryVisualService {

    private static final Logger log = LoggerFactory.getLogger(StoryVisualService.class);

    private final ImageGenerationService primaryImageService;
    private final PlaceholderImageService placeholderService;
    private final StoryRepository storyRepository;
    private final StorySceneRepository sceneRepository;
    private final GeneratedAssetRepository assetRepository;
    private final AssetStorageService storageService;

    private final int defaultWidth;
    private final int defaultHeight;
    private final int defaultSteps;
    private final double defaultCfgScale;

    public StoryVisualService(
            ImageGenerationService primaryImageService,
            @Qualifier("placeholderImageService") PlaceholderImageService placeholderService,
            StoryRepository storyRepository,
            StorySceneRepository sceneRepository,
            GeneratedAssetRepository assetRepository,
            AssetStorageService storageService,
            @Value("${story.image.width:768}") int defaultWidth,
            @Value("${story.image.height:1344}") int defaultHeight,
            @Value("${story.image.steps:25}") int defaultSteps,
            @Value("${story.image.cfg-scale:7.0}") double defaultCfgScale) {
        this.primaryImageService = primaryImageService;
        this.placeholderService = placeholderService;
        this.storyRepository = storyRepository;
        this.sceneRepository = sceneRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.defaultSteps = defaultSteps;
        this.defaultCfgScale = defaultCfgScale;
    }

    @Transactional
    public StoryResponseDto generateImagesForStory(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NoSuchElementException("Historia no encontrada con ID: " + storyId));

        log.info("Iniciando generación de imágenes para historia '{}' (ID: {})", story.getTitle(), storyId);
        story.setStatus(StoryStatus.GENERATING_ASSETS);

        long baseSeed = Math.abs(new Random().nextLong() % 100000000L);
        List<StoryScene> scenes = sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId);

        for (StoryScene scene : scenes) {
            generateSceneImageInternal(story, scene, baseSeed + scene.getSequenceNumber());
        }

        storyRepository.save(story);
        return StoryResponseDto.fromEntity(story, storageService);
    }

    @Transactional
    public StorySceneResponseDto regenerateSceneImage(UUID storyId, UUID sceneId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NoSuchElementException("Historia no encontrada con ID: " + storyId));

        StoryScene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new NoSuchElementException("Escena no encontrada con ID: " + sceneId));

        if (scene.getStory() != null && scene.getStory().getId() != null && !scene.getStory().getId().equals(storyId)) {
            throw new IllegalArgumentException("La escena " + sceneId + " no pertenece a la historia " + storyId);
        }

        scene.setImageVersion(scene.getImageVersion() + 1);
        long newSeed = Math.abs(new Random().nextLong() % 100000000L);

        generateSceneImageInternal(story, scene, newSeed);
        return StorySceneResponseDto.fromEntity(scene, storageService);
    }

    private void generateSceneImageInternal(Story story, StoryScene scene, long seed) {
        scene.setStatus(SceneStatus.GENERATING_IMAGE);

        String fullPrompt = buildEnrichedPrompt(story, scene);
        String negativePrompt = story.getVisualStyle() != null
                ? story.getVisualStyle().getNegativePrompt()
                : "blurry, low quality, cartoon, deformed";

        ImageGenerationRequest request = new ImageGenerationRequest(
                story.getId(),
                scene.getId(),
                scene.getSequenceNumber(),
                fullPrompt,
                negativePrompt,
                seed,
                defaultWidth,
                defaultHeight,
                defaultSteps,
                defaultCfgScale,
                "DPM++ 2M Karras"
        );

        GeneratedImage image;
        try {
            if (primaryImageService.isAvailable()) {
                image = primaryImageService.generate(request);
            } else {
                log.warn("Servicio primario de imágenes ({}) no disponible. Usando fallback de placeholder.", primaryImageService.getProviderName());
                image = placeholderService.generate(request);
            }
        } catch (Exception e) {
            log.error("Fallo generando imagen con {}: {}. Conmutando a fallback de placeholder.", primaryImageService.getProviderName(), e.getMessage());
            image = placeholderService.generate(request);
        }

        // Guardar archivo físico en almacenamiento
        String relativePath = String.format("stories/%s/images/scene_%02d_v%d.png",
                story.getId(), scene.getSequenceNumber(), scene.getImageVersion());

        storageService.store(relativePath, image.imageBytes());

        // Registrar GeneratedAsset
        GeneratedAsset asset = new GeneratedAsset(
                scene,
                AssetType.IMAGE,
                relativePath,
                image.mimeType(),
                image.imageBytes().length,
                scene.getImageVersion()
        );
        assetRepository.save(asset);

        // Actualizar escena
        scene.setImagePath(relativePath);
        scene.setStatus(SceneStatus.IMAGE_READY);
        sceneRepository.save(scene);

        log.info("Escena {} actualizada con imagen en {}", scene.getSequenceNumber(), relativePath);
    }

    private String buildEnrichedPrompt(Story story, StoryScene scene) {
        StringBuilder sb = new StringBuilder();

        if (scene.getVisualPrompt() != null && !scene.getVisualPrompt().isBlank()) {
            sb.append(scene.getVisualPrompt().trim());
        }

        // Inyectar fragmentos de personajes si aplican
        if (story.getCharacters() != null) {
            for (StoryCharacter ch : story.getCharacters()) {
                if (ch.getPromptFragment() != null && !ch.getPromptFragment().isBlank()) {
                    sb.append(", featuring ").append(ch.getPromptFragment().trim());
                }
            }
        }

        // Inyectar modificadores del estilo visual
        if (story.getVisualStyle() != null) {
            if (story.getVisualStyle().getArtStyle() != null) {
                sb.append(", ").append(story.getVisualStyle().getArtStyle());
            }
            if (story.getVisualStyle().getQualityModifiers() != null) {
                sb.append(", ").append(story.getVisualStyle().getQualityModifiers());
            }
        }

        return sb.toString();
    }
}
