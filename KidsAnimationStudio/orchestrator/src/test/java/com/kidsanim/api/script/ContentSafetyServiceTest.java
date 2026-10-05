package com.kidsanim.api.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.ScenePurpose;
import com.kidsanim.api.infrastructure.config.KidsSafetyProperties;
import com.kidsanim.api.llm.LlmProvider;
import com.kidsanim.api.script.model.EpisodeScript;
import com.kidsanim.api.script.model.PedagogicalReviewResult;
import com.kidsanim.api.script.model.SceneScript;
import com.kidsanim.api.script.model.ShotScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentSafetyServiceTest {

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private PromptTemplateService promptTemplateService;

    private ContentSafetyService contentSafetyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        KidsSafetyProperties safetyProperties = new KidsSafetyProperties(
                "classpath:content-safety-blocklist.txt",
                14,
                4,
                8,
                12,
                40,
                2
        );

        contentSafetyService = new ContentSafetyService(
                safetyProperties,
                new DefaultResourceLoader(),
                promptTemplateService,
                llmProvider,
                objectMapper
        );
        contentSafetyService.init();
    }

    @Test
    void rejectsScriptWithProhibitedWordsInNarration() {
        ShotScript shot = new ShotScript("tito", "tito", "¡Cuidado con el monstruo feo!",
                "forest", "running away", CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME, List.of(), List.of(), 500);
        SceneScript scene = new SceneScript(ScenePurpose.INTRO, "bosque", "tense", List.of(shot));
        EpisodeScript script = new EpisodeScript("Miedo en el bosque", "Aprender", List.of(scene));

        List<String> issues = contentSafetyService.checkBlocklist(script);

        assertThat(issues).isNotEmpty();
        assertThat(issues).anyMatch(issue -> issue.contains("monstruo") || issue.contains("miedo"));
    }

    @Test
    void rejectsScriptWithExcessiveWordsPerNarration() {
        String longNarration = "Uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince dieciseis";
        ShotScript shot = new ShotScript("tito", "tito", longNarration,
                "tree", "idle", CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME, List.of(), List.of(), 500);
        SceneScript scene = new SceneScript(ScenePurpose.INTRO, "bosque", "calm", List.of(shot));
        EpisodeScript script = new EpisodeScript("Contando", "Contar", List.of(scene));

        List<String> issues = contentSafetyService.checkNarrationLength(script);

        assertThat(issues).isNotEmpty();
        assertThat(issues.get(0)).contains("excede el límite de 14 palabras");
    }

    @Test
    void approvesSafeAndConciseScriptWithoutLlm() {
        ShotScript shot = new ShotScript("tito", "tito", "¡Hola amiguitos! Vamos a jugar juntos.",
                "sunny tree", "waving happily", CameraMotion.SLOW_ZOOM_IN, ContinuityMode.NEW_KEYFRAME, List.of(), List.of(), 500);
        SceneScript scene = new SceneScript(ScenePurpose.INTRO, "huerto", "happy", List.of(shot));
        EpisodeScript script = new EpisodeScript("Jugamos con Tito", "Jugar", List.of(scene));

        PedagogicalReviewResult result = contentSafetyService.evaluate(script, false);

        assertThat(result.approved()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void rejectsWhenLlmPedagogicalReviewFails() {
        when(llmProvider.isAvailable()).thenReturn(true);
        when(promptTemplateService.getReviewerPrompt()).thenReturn("Reviewer prompt");
        when(promptTemplateService.getPedagogicalReviewSchema()).thenReturn(Map.of("type", "object"));
        when(llmProvider.generateStructured(anyString(), anyString(), any()))
                .thenReturn("{\"approved\": false, \"issues\": [\"El vocabulario es abstracto para niños de 3 años\"]}");

        ShotScript shot = new ShotScript("tito", "tito", "Hola amigos.",
                "sunny tree", "waving", CameraMotion.STATIC, ContinuityMode.NEW_KEYFRAME, List.of(), List.of(), 500);
        SceneScript scene = new SceneScript(ScenePurpose.INTRO, "huerto", "happy", List.of(shot));
        EpisodeScript script = new EpisodeScript("Tito", "Aprender", List.of(scene));

        PedagogicalReviewResult result = contentSafetyService.evaluate(script, true);

        assertThat(result.approved()).isFalse();
        assertThat(result.issues()).contains("El vocabulario es abstracto para niños de 3 años");
    }
}
