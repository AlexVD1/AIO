package com.storyvideo.api.service;

import com.storyvideo.api.ai.StoryAiClient;
import com.storyvideo.api.ai.dto.AiCharacterItem;
import com.storyvideo.api.ai.dto.AiSceneItem;
import com.storyvideo.api.ai.dto.AiStoryResponse;
import com.storyvideo.api.domain.*;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.StoryFingerprintRepository;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.VisualStyleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryGenerationServiceTest {

    @Mock
    private StoryAiClient aiClient;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private VisualStyleRepository visualStyleRepository;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private AssetStorageService storageService;

    private StoryValidationService validationService;
    private StoryGenerationService generationService;

    @BeforeEach
    void setUp() {
        validationService = new StoryValidationService();
        generationService = new StoryGenerationService(
                aiClient,
                validationService,
                storyRepository,
                visualStyleRepository,
                deduplicationService,
                storageService
        );
    }

    @Test
    @DisplayName("Debe generar y persistir una historia estructurada completa")
    void generateAndPersistStory_Success() {
        StoryGenerationRequestDto request = new StoryGenerationRequestDto(
                StoryGenre.HORROR,
                StoryTone.DARK,
                "Hospital abandonado",
                null,
                90,
                "es-MX",
                4,
                null,
                null
        );

        AiStoryResponse mockAiResponse = new AiStoryResponse(
                "La Sala 404",
                "Hay una habitación en este hospital que no aparece en ningún plano oficial.",
                "Un guardia nocturno investiga ruidos en el ala cerrada",
                "Terror psicológico en un hospital",
                NarrativeArc.LINEAR,
                "El guardia era el paciente original de 1974",
                "La puerta se cierra desde afuera",
                List.of(new AiCharacterItem("Marcos", "Guardia de 40 años", "Cicatriz en la ceja", "PROTAGONIST", "middle-aged security guard")),
                List.of(
                        new AiSceneItem(1, SceneType.HOOK, "El monitor de seguridad parpadeó a las 3:15 AM.", "TENSE", "Monitores en la garita", "dark security room with glowing monitors, 35mm film", CameraMovement.SLOW_ZOOM_IN, "MEDIUM", null, TransitionType.FADE_IN, TransitionType.CUT),
                        new AiSceneItem(2, SceneType.DEVELOPMENT, "Caminó por el pasillo del ala norte con su linterna.", "SUSPENSE", "Pasillo oscuro", "narrow dark hallway lit only by a flashlight beam, volumetric fog", CameraMovement.KEN_BURNS, "HIGH", null, TransitionType.CUT, TransitionType.CUT),
                        new AiSceneItem(3, SceneType.CLIMAX, "Frente a él apareció una puerta marcada con tiza: 404.", "FEAR", "Puerta marcada", "creepy wooden door with 404 written in chalk, hyperrealistic, 8k", CameraMovement.SLOW_ZOOM_IN, "CRESCENDO", null, TransitionType.CUT, TransitionType.FADE_OUT),
                        new AiSceneItem(4, SceneType.RESOLUTION, "Al entrar, encontró su propio uniforme colgado de la pared.", "WHISPER", "Uniforme en habitación vacía", "empty decaying hospital room with single security uniform hanging, ominous", CameraMovement.STATIC, "LOW", null, TransitionType.FADE_IN, TransitionType.FADE_OUT)
                )
        );

        VisualStyle defaultStyle = new VisualStyle("Cinematic Dark", "cinematic", "cartoon", "8k", null, true);
        when(visualStyleRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultStyle));
        when(storyRepository.findRecentPremisesByGenre(any(), anyInt())).thenReturn(List.of());
        when(aiClient.generateStory(any())).thenReturn(mockAiResponse);
        when(deduplicationService.checkDeduplication(any(), any(), any(), any(), any()))
                .thenReturn(com.storyvideo.api.dto.DeduplicationCheckResult.ok());
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoryResponseDto response = generationService.generateAndPersistStory(request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("La Sala 404");
        assertThat(response.genre()).isEqualTo(StoryGenre.HORROR);
        assertThat(response.scenes()).hasSize(4);
        assertThat(response.characters()).hasSize(1);
        assertThat(response.status()).isEqualTo(StoryStatus.PLANNING_SCENES);

        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepository).save(storyCaptor.capture());
        Story saved = storyCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo("La Sala 404");
        assertThat(saved.getScenes()).hasSize(4);

        verify(deduplicationService).saveFingerprint(any(Story.class), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la historia generada es rechazada por deduplicación")
    void generateAndPersistStory_Duplicate_ThrowsException() {
        StoryGenerationRequestDto request = new StoryGenerationRequestDto(
                StoryGenre.HORROR, StoryTone.DARK, "Hospital abandonado", null, 90, "es-MX", 4, null, null
        );

        AiStoryResponse mockAiResponse = new AiStoryResponse(
                "La Sala 404",
                "El número de la puerta comenzó a sangrar lentamente.",
                "Premisa duplicada",
                "Sinopsis",
                NarrativeArc.LINEAR,
                "Giro",
                "Final",
                List.of(),
                List.of(
                        new AiSceneItem(1, SceneType.HOOK, "Texto 1", "TENSE", "D1", "dark security room with glowing monitors, 35mm film", CameraMovement.SLOW_ZOOM_IN, "HIGH", null, TransitionType.CUT, TransitionType.CUT),
                        new AiSceneItem(2, SceneType.DEVELOPMENT, "Texto 2", "TENSE", "D2", "narrow dark hallway lit only by a flashlight beam", CameraMovement.SLOW_ZOOM_IN, "HIGH", null, TransitionType.CUT, TransitionType.CUT),
                        new AiSceneItem(3, SceneType.CLIMAX, "Texto 3", "TENSE", "D3", "creepy wooden door with 404 written in chalk, 8k", CameraMovement.SLOW_ZOOM_IN, "HIGH", null, TransitionType.CUT, TransitionType.CUT)
                )
        );

        when(storyRepository.findRecentPremisesByGenre(any(), anyInt())).thenReturn(List.of());
        when(aiClient.generateStory(any())).thenReturn(mockAiResponse);
        when(deduplicationService.checkDeduplication(any(), any(), any(), any(), any()))
                .thenReturn(com.storyvideo.api.dto.DeduplicationCheckResult.duplicate("Título idéntico", "La Sala 404"));

        assertThatThrownBy(() -> generationService.generateAndPersistStory(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rechazada por duplicación");
    }

    @Test
    @DisplayName("Debe lanzar excepción si la IA genera una historia que no supera la validación")
    void generateAndPersistStory_InvalidNarrative_ThrowsException() {
        StoryGenerationRequestDto request = new StoryGenerationRequestDto(
                StoryGenre.HORROR,
                StoryTone.DARK,
                "Test",
                null,
                90,
                "es-MX",
                4,
                null,
                null
        );

        AiStoryResponse invalidResponse = new AiStoryResponse(
                "", // Título vacío
                "", // Hook vacío
                "",
                "",
                NarrativeArc.LINEAR,
                null,
                "",
                List.of(),
                List.of()
        );

        when(visualStyleRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());
        when(storyRepository.findRecentPremisesByGenre(any(), anyInt())).thenReturn(List.of());
        when(aiClient.generateStory(any())).thenReturn(invalidResponse);

        assertThatThrownBy(() -> generationService.generateAndPersistStory(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no superó los controles de calidad");

        verify(storyRepository, never()).save(any());
    }
}
