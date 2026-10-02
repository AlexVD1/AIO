package com.storyvideo.api.service;

import com.storyvideo.api.audio.narration.NarrationService;
import com.storyvideo.api.audio.narration.NoOpNarrationService;
import com.storyvideo.api.audio.narration.dto.NarrationResult;
import com.storyvideo.api.domain.*;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.dto.StorySceneResponseDto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryAudioServiceTest {

    @Mock
    private NarrationService primaryNarrationService;

    @Mock
    private NoOpNarrationService noOpNarrationService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StorySceneRepository sceneRepository;

    @Mock
    private GeneratedAssetRepository assetRepository;

    @Mock
    private AssetStorageService storageService;

    private StoryAudioService audioService;

    @BeforeEach
    void setUp() {
        audioService = new StoryAudioService(
                primaryNarrationService,
                noOpNarrationService,
                storyRepository,
                sceneRepository,
                assetRepository,
                storageService
        );
    }

    @Test
    @DisplayName("Debe sintetizar locución para todas las escenas y sincronizar offsets del timeline")
    void generateAudioForStory_Success() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("El Pasadizo", StoryGenre.HORROR, null, StoryTone.DARK, null, "Hook", "Premisa", "Sinopsis", NarrativeArc.LINEAR, null, "Final", "es-MX", 60, null);
        story.setId(storyId);

        StoryScene scene1 = new StoryScene(1, SceneType.HOOK, "El pasadizo apareció a la medianoche.", "Desc 1", "prompt 1", CameraMovement.SLOW_ZOOM_IN);
        scene1.setId(UUID.randomUUID());
        scene1.setStory(story);
        scene1.setImagePath("stories/" + storyId + "/images/scene_01_v1.png"); // Ya tiene imagen
        scene1.setStatus(SceneStatus.IMAGE_READY);

        StoryScene scene2 = new StoryScene(2, SceneType.CLIMAX, "Nadie pudo encontrar la salida después.", "Desc 2", "prompt 2", CameraMovement.KEN_BURNS);
        scene2.setId(UUID.randomUUID());
        scene2.setStory(story);
        scene2.setStatus(SceneStatus.PLANNED); // Sin imagen aún

        story.addScene(scene1);
        story.addScene(scene2);

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId)).thenReturn(List.of(scene1, scene2));
        when(primaryNarrationService.isAvailable()).thenReturn(true);

        NarrationResult res1 = NarrationResult.mp3(new byte[]{1, 2, 3}, 4.0, "es-MX-JorgeNeural");
        NarrationResult res2 = NarrationResult.mp3(new byte[]{4, 5, 6}, 5.0, "es-MX-JorgeNeural");
        when(primaryNarrationService.synthesize(any())).thenReturn(res1, res2);

        StoryResponseDto response = audioService.generateAudioForStory(storyId, null);

        assertThat(response).isNotNull();

        // Escena 1 tenía imagen, por lo que debe pasar a READY
        assertThat(scene1.getStatus()).isEqualTo(SceneStatus.READY);
        assertThat(scene1.getNarrationDurationSeconds()).isEqualByComparingTo(BigDecimal.valueOf(4.00));
        assertThat(scene1.getStartTimeSeconds()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scene1.getEndTimeSeconds()).isEqualByComparingTo(BigDecimal.valueOf(4.75));

        // Escena 2 no tenía imagen, por lo que pasa a AUDIO_READY
        assertThat(scene2.getStatus()).isEqualTo(SceneStatus.AUDIO_READY);
        assertThat(scene2.getNarrationDurationSeconds()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(scene2.getStartTimeSeconds()).isEqualByComparingTo(BigDecimal.valueOf(4.75));
        assertThat(scene2.getEndTimeSeconds()).isEqualByComparingTo(BigDecimal.valueOf(10.50));

        // Story targetDuration actualizado con la suma
        assertThat(story.getTargetDurationSeconds()).isEqualTo(11);

        verify(storageService, times(2)).store(anyString(), any(byte[].class));
        verify(assetRepository, times(2)).save(any(GeneratedAsset.class));
        verify(storyRepository).save(story);
    }

    @Test
    @DisplayName("Debe conmutar transparentemente a fallback NoOp si el servicio primario falla")
    void generateAudioForStory_FallbackToNoOpOnFailure() {
        UUID storyId = UUID.randomUUID();
        Story story = new Story("Historia", StoryGenre.MYSTERY, null, StoryTone.SUSPENSE, null, "H", "P", "S", NarrativeArc.LINEAR, null, "F", "es-MX", 60, null);
        story.setId(storyId);

        StoryScene scene = new StoryScene(1, SceneType.HOOK, "Texto de prueba", "Desc", "prompt", CameraMovement.STATIC);
        scene.setId(UUID.randomUUID());
        scene.setStory(story);
        story.addScene(scene);

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId)).thenReturn(List.of(scene));
        when(primaryNarrationService.isAvailable()).thenReturn(false); // TTS no disponible

        NarrationResult noOpRes = NarrationResult.mp3(new byte[]{9, 9}, 4.2, "simulated-voice");
        when(noOpNarrationService.synthesize(any())).thenReturn(noOpRes);

        StoryResponseDto response = audioService.generateAudioForStory(storyId, null);

        assertThat(response).isNotNull();
        assertThat(scene.getStatus()).isEqualTo(SceneStatus.AUDIO_READY);
        verify(noOpNarrationService).synthesize(any());
        verify(primaryNarrationService, never()).synthesize(any());
    }

    @Test
    @DisplayName("Debe regenerar la locución de una escena puntual y sincronizar el timeline")
    void regenerateSceneAudio_Success() {
        UUID storyId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        Story story = new Story("Historia", StoryGenre.HORROR, null, StoryTone.DARK, null, "H", "P", "S", NarrativeArc.LINEAR, null, "F", "es-MX", 60, null);
        story.setId(storyId);

        StoryScene scene = new StoryScene(1, SceneType.HOOK, "Texto modificado", "Desc", "prompt", CameraMovement.STATIC);
        scene.setId(sceneId);
        scene.setStory(story);

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(sceneRepository.findById(sceneId)).thenReturn(Optional.of(scene));
        when(sceneRepository.findByStoryIdOrderBySequenceNumberAsc(storyId)).thenReturn(List.of(scene));
        when(primaryNarrationService.isAvailable()).thenReturn(true);

        NarrationResult newResult = NarrationResult.mp3(new byte[]{7, 8, 9}, 3.5, "es-MX-DaliaNeural");
        when(primaryNarrationService.synthesize(any())).thenReturn(newResult);

        StorySceneResponseDto result = audioService.regenerateSceneAudio(storyId, sceneId, "es-MX-DaliaNeural");

        assertThat(result).isNotNull();
        assertThat(scene.getNarrationDurationSeconds()).isEqualByComparingTo(BigDecimal.valueOf(3.50));
        verify(storageService).store(contains("scene_01.mp3"), any(byte[].class));
    }
}
