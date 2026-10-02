package com.storyvideo.api.service;

import com.storyvideo.api.domain.*;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.dto.StorySceneResponseDto;
import com.storyvideo.api.image.ImageGenerationService;
import com.storyvideo.api.image.PlaceholderImageService;
import com.storyvideo.api.image.dto.GeneratedImage;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.GeneratedAssetRepository;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.StorySceneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryVisualServiceTest {

    @Mock
    private ImageGenerationService primaryImageService;

    @Mock
    private PlaceholderImageService placeholderService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StorySceneRepository sceneRepository;

    @Mock
    private GeneratedAssetRepository assetRepository;

    @Mock
    private AssetStorageService storageService;

    private StoryVisualService visualService;

    @BeforeEach
    void setUp() {
        visualService = new StoryVisualService(
                primaryImageService,
                placeholderService,
                storyRepository,
                sceneRepository,
                assetRepository,
                storageService,
                768,
                1344,
                25,
                7.0
        );
    }

    @Test
    @DisplayName("Debe generar y almacenar imágenes para todas las escenas de la historia")
    void generateImagesForStory_Success() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story(
                "Noche en el Faro",
                StoryGenre.MYSTERY,
                null,
                StoryTone.SUSPENSE,
                "Faro aislado",
                "Hook de faro",
                "Premisa de faro",
                "Sinopsis",
                NarrativeArc.LINEAR,
                null,
                "Desenlace",
                "es-MX",
                60,
                null
        );
        story.setId(storyId);

        StoryScene scene1 = new StoryScene(1, SceneType.HOOK, "Locución 1", "Faro en la niebla", "cinematic shot of foggy lighthouse", CameraMovement.SLOW_ZOOM_IN);
        scene1.setStory(story);

        StoryScene scene2 = new StoryScene(2, SceneType.CLIMAX, "Locución 2", "Luz apagándose", "creepy lantern turning off", CameraMovement.KEN_BURNS);
        scene2.setStory(story);

        story.addScene(scene1);
        story.addScene(scene2);

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId)).thenReturn(List.of(scene1, scene2));
        when(primaryImageService.isAvailable()).thenReturn(true);

        GeneratedImage dummyImage = GeneratedImage.png(new byte[]{1, 2, 3, 4}, 55555L, 768, 1344, 200);
        when(primaryImageService.generate(any())).thenReturn(dummyImage);

        StoryResponseDto response = visualService.generateImagesForStory(storyId);

        assertThat(response).isNotNull();
        assertThat(scene1.getStatus()).isEqualTo(SceneStatus.IMAGE_READY);
        assertThat(scene2.getStatus()).isEqualTo(SceneStatus.IMAGE_READY);
        assertThat(scene1.getImagePath()).contains("scene_01_v1.png");
        assertThat(scene2.getImagePath()).contains("scene_02_v1.png");

        verify(storageService, times(2)).store(anyString(), any(byte[].class));
        verify(assetRepository, times(2)).save(any(GeneratedAsset.class));
        verify(storyRepository).save(story);
    }

    @Test
    @DisplayName("Debe conmutar transparentemente a Placeholder si el servicio primario falla")
    void generateImagesForStory_FallbackToPlaceholderOnFailure() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story(
                "Noche en el Faro",
                StoryGenre.MYSTERY,
                null,
                StoryTone.SUSPENSE,
                null,
                "Hook",
                "Premisa",
                "Sinopsis",
                NarrativeArc.LINEAR,
                null,
                "Fin",
                "es-MX",
                60,
                null
        );
        story.setId(storyId);

        StoryScene scene1 = new StoryScene(1, SceneType.HOOK, "Locución 1", "Faro", "foggy lighthouse", CameraMovement.SLOW_ZOOM_IN);
        scene1.setStory(story);
        story.addScene(scene1);

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId)).thenReturn(List.of(scene1));
        when(primaryImageService.isAvailable()).thenReturn(false); // Forge no disponible

        GeneratedImage placeholderImage = GeneratedImage.png(new byte[]{9, 9, 9}, 11111L, 768, 1344, 10);
        when(placeholderService.generate(any())).thenReturn(placeholderImage);

        StoryResponseDto response = visualService.generateImagesForStory(storyId);

        assertThat(response).isNotNull();
        assertThat(scene1.getStatus()).isEqualTo(SceneStatus.IMAGE_READY);
        verify(placeholderService).generate(any());
        verify(primaryImageService, never()).generate(any());
    }

    @Test
    @DisplayName("Debe regenerar la imagen de una escena individual incrementando su versión")
    void regenerateSceneImage_Success() {
        UUID storyId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        Story story = new Story("Historia", StoryGenre.HORROR, null, StoryTone.DARK, null, "H", "P", "S", NarrativeArc.LINEAR, null, "E", "es-MX", 60, null);
        story.setId(storyId);

        StoryScene scene = new StoryScene(1, SceneType.HOOK, "Locución", "Desc", "prompt", CameraMovement.STATIC);
        scene.setId(sceneId);
        scene.setStory(story);
        scene.setImageVersion(1);

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(sceneRepository.findById(sceneId)).thenReturn(Optional.of(scene));
        when(primaryImageService.isAvailable()).thenReturn(true);

        GeneratedImage dummyImage = GeneratedImage.png(new byte[]{4, 5, 6}, 99999L, 768, 1344, 150);
        when(primaryImageService.generate(any())).thenReturn(dummyImage);

        StorySceneResponseDto result = visualService.regenerateSceneImage(storyId, sceneId);

        assertThat(result).isNotNull();
        assertThat(scene.getImageVersion()).isEqualTo(2);
        assertThat(scene.getImagePath()).contains("scene_01_v2.png");
        verify(storageService).store(contains("scene_01_v2.png"), any(byte[].class));
    }
}
