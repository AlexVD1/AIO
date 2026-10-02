package com.storyvideo.api.service;

import com.storyvideo.api.ai.dto.AiCharacterItem;
import com.storyvideo.api.ai.dto.AiSceneItem;
import com.storyvideo.api.ai.dto.AiStoryResponse;
import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.NarrativeArc;
import com.storyvideo.api.domain.SceneType;
import com.storyvideo.api.domain.TransitionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoryValidationServiceTest {

    private StoryValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new StoryValidationService();
    }

    @Test
    @DisplayName("Historia válida con hook, final y 4 escenas debe pasar la validación")
    void validate_ValidStory_ShouldPass() {
        AiStoryResponse validStory = new AiStoryResponse(
                "El Piso Oculto",
                "¿Por qué el elevador siempre se detiene entre los pisos 7 y 8?",
                "Una trabajadora nocturna descubre un piso secreto",
                "Un thriller psicológico sobre una planta clausurada",
                NarrativeArc.LINEAR,
                "Ella misma era quien construyó ese piso hace 30 años",
                "Las puertas se abren y la oscuridad la recibe",
                List.of(new AiCharacterItem("Elena", "Mujer de 35 años, cabello recogido", "Ojos cansados", "PROTAGONIST", "woman in her 30s")),
                List.of(
                        new AiSceneItem(1, SceneType.HOOK, "El elevador nunca miente, excepto a las 3 AM.", "TENSE", "Elena en el elevador", "cinematic photo of woman inside dark elevator, dim lights", CameraMovement.SLOW_ZOOM_IN, "HIGH", null, TransitionType.FADE_IN, TransitionType.CUT),
                        new AiSceneItem(2, SceneType.DEVELOPMENT, "Las luces parpadearon y los botones se apagaron.", "UNSETTLING", "Panel apagado", "close up of elevator buttons flickering dark, 35mm film", CameraMovement.KEN_BURNS, "MEDIUM", null, TransitionType.CUT, TransitionType.CUT),
                        new AiSceneItem(3, SceneType.CLIMAX, "Una puerta oxidada se entreabrió en la penumbra.", "FEAR", "Puerta entreabierta", "creepy rusted door opening in darkness, atmospheric dust particles", CameraMovement.SLOW_ZOOM_IN, "HIGH", null, TransitionType.CUT, TransitionType.FADE_OUT),
                        new AiSceneItem(4, SceneType.RESOLUTION, "Nadie volvió a ver a Elena al día siguiente.", "WHISPER", "Pasillo vacío", "empty desolate hallway with single light bulb swinging, 8k", CameraMovement.STATIC, "LOW", null, TransitionType.FADE_IN, TransitionType.FADE_OUT)
                )
        );

        StoryValidationService.ValidationResult result = validationService.validate(validStory);
        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("Historia sin hook debe ser rechazada")
    void validate_MissingHook_ShouldFail() {
        AiStoryResponse storyWithoutHook = new AiStoryResponse(
                "Sin Hook",
                "",
                "Premisa",
                "Sinopsis",
                NarrativeArc.LINEAR,
                null,
                "Final",
                List.of(),
                List.of(
                        new AiSceneItem(1, SceneType.HOOK, "Texto 1", "NEUTRAL", "Desc 1", "cinematic shot 1", CameraMovement.STATIC, "LOW", null, TransitionType.CUT, TransitionType.CUT),
                        new AiSceneItem(2, SceneType.DEVELOPMENT, "Texto 2", "NEUTRAL", "Desc 2", "cinematic shot 2", CameraMovement.STATIC, "LOW", null, TransitionType.CUT, TransitionType.CUT),
                        new AiSceneItem(3, SceneType.RESOLUTION, "Texto 3", "NEUTRAL", "Desc 3", "cinematic shot 3", CameraMovement.STATIC, "LOW", null, TransitionType.CUT, TransitionType.CUT)
                )
        );

        StoryValidationService.ValidationResult result = validationService.validate(storyWithoutHook);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("hook inicial"));
    }

    @Test
    @DisplayName("Historia con menos de 3 escenas debe ser rechazada")
    void validate_TooFewScenes_ShouldFail() {
        AiStoryResponse storyWith2Scenes = new AiStoryResponse(
                "Muy Corta",
                "Hook impactante de prueba",
                "Premisa",
                "Sinopsis",
                NarrativeArc.LINEAR,
                null,
                "Final",
                List.of(),
                List.of(
                        new AiSceneItem(1, SceneType.HOOK, "Texto 1", "NEUTRAL", "Desc 1", "cinematic shot 1", CameraMovement.STATIC, "LOW", null, TransitionType.CUT, TransitionType.CUT),
                        new AiSceneItem(2, SceneType.RESOLUTION, "Texto 2", "NEUTRAL", "Desc 2", "cinematic shot 2", CameraMovement.STATIC, "LOW", null, TransitionType.CUT, TransitionType.CUT)
                )
        );

        StoryValidationService.ValidationResult result = validationService.validate(storyWith2Scenes);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("al menos 3 escenas"));
    }
}
