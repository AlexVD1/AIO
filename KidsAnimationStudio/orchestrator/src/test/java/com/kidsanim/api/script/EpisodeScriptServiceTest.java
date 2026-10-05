package com.kidsanim.api.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.ScenePurpose;
import com.kidsanim.api.dto.CreateEpisodeRequest;
import com.kidsanim.api.infrastructure.exception.ScriptValidationException;
import com.kidsanim.api.llm.LlmProvider;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.SceneRepository;
import com.kidsanim.api.repository.SeriesRepository;
import com.kidsanim.api.repository.ShotRepository;
import com.kidsanim.api.script.model.EpisodeScript;
import com.kidsanim.api.script.model.OverlaySpec;
import com.kidsanim.api.script.model.PedagogicalReviewResult;
import com.kidsanim.api.script.model.SceneScript;
import com.kidsanim.api.script.model.ShotScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodeScriptServiceTest {

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private SceneRepository sceneRepository;

    @Mock
    private ShotRepository shotRepository;

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private ScriptSchemaValidator schemaValidator;

    @Mock
    private ContentSafetyService contentSafetyService;

    @Mock
    private EpisodeFingerprintService fingerprintService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private EpisodeScriptService episodeScriptService;
    private Series series;
    private UUID seriesId;

    @BeforeEach
    void setUp() {
        episodeScriptService = new EpisodeScriptService(
                seriesRepository,
                episodeRepository,
                sceneRepository,
                shotRepository,
                llmProvider,
                promptTemplateService,
                schemaValidator,
                contentSafetyService,
                fingerprintService,
                objectMapper
        );

        seriesId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        series = new Series("Tito el zorrito", "Serie preescolar", styleProfile);
        Character tito = new Character(series, "Tito", CharacterRole.HOST, "cute red fox");
        Location huerto = new Location(series, "Huerto", "huerto de manzanas soleado");
        series.getCharacters().add(tito);
        series.getLocations().add(huerto);
    }

    private EpisodeScript createSampleScript() {
        List<SceneScript> scenes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            List<ShotScript> shots = new ArrayList<>();
            for (int j = 1; j <= 3; j++) {
                shots.add(new ShotScript(
                        "tito",
                        "Tito",
                        "¡Contamos manzanas!",
                        "Tito in orchard",
                        "smiling",
                        CameraMotion.STATIC,
                        ContinuityMode.NEW_KEYFRAME,
                        List.of(new OverlaySpec("BIG_NUMBER", "1", "TOP_RIGHT", null)),
                        List.of("pop"),
                        300
                ));
            }
            scenes.add(new SceneScript(ScenePurpose.INTRO, "Huerto", "happy", shots));
        }
        return new EpisodeScript("¡Contamos con Tito!", "Aprender números", scenes);
    }

    @Test
    void generatesAndSavesEpisodeSuccessfullyWithAutoApprove() throws Exception {
        CreateEpisodeRequest request = new CreateEpisodeRequest(
                EducationalTopicType.COUNTING,
                "Contar del 1 al 5",
                "Reconocer números del 1 al 5",
                120,
                true
        );

        EpisodeScript script = createSampleScript();
        String json = objectMapper.writeValueAsString(script);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(llmProvider.getProviderName()).thenReturn("ollama-qwen2.5");
        when(promptTemplateService.getSystemPrompt()).thenReturn("System prompt");
        when(promptTemplateService.buildUserPrompt(any(), any(), any(), any(), anyInt())).thenReturn("User prompt");
        when(promptTemplateService.getEpisodeScriptSchema()).thenReturn(Map.of("type", "object"));
        when(llmProvider.generateStructured(anyString(), anyString(), any())).thenReturn(json);

        when(schemaValidator.validate(any(), any(), any())).thenReturn(List.of());
        when(contentSafetyService.evaluate(any(), anyBoolean())).thenReturn(PedagogicalReviewResult.ok());
        when(fingerprintService.computeFingerprint(any(), any(), any(), any())).thenReturn("hash12345");
        when(episodeRepository.findByFingerprint("hash12345")).thenReturn(Optional.empty());

        when(episodeRepository.save(any(Episode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sceneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Episode result = episodeScriptService.generateAndSaveEpisode(seriesId, request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("¡Contamos con Tito!");
        assertThat(result.getStatus()).isEqualTo(EpisodeStatus.NARRATION);
        assertThat(result.getFingerprint()).isEqualTo("hash12345");
        assertThat(result.getScenes()).hasSize(4);

        verify(episodeRepository).save(any(Episode.class));
        verify(sceneRepository, times(4)).save(any());
        verify(shotRepository, times(12)).save(any());
    }

    @Test
    void setsStatusToAwaitingScriptApprovalWhenAutoApproveIsFalse() throws Exception {
        CreateEpisodeRequest request = new CreateEpisodeRequest(
                EducationalTopicType.COUNTING,
                "Contar del 1 al 5",
                "Reconocer números del 1 al 5",
                120,
                false
        );

        EpisodeScript script = createSampleScript();
        String json = objectMapper.writeValueAsString(script);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(llmProvider.getProviderName()).thenReturn("ollama-qwen2.5");
        when(promptTemplateService.getSystemPrompt()).thenReturn("System prompt");
        when(promptTemplateService.buildUserPrompt(any(), any(), any(), any(), anyInt())).thenReturn("User prompt");
        when(promptTemplateService.getEpisodeScriptSchema()).thenReturn(Map.of("type", "object"));
        when(llmProvider.generateStructured(anyString(), anyString(), any())).thenReturn(json);

        when(schemaValidator.validate(any(), any(), any())).thenReturn(List.of());
        when(contentSafetyService.evaluate(any(), anyBoolean())).thenReturn(PedagogicalReviewResult.ok());
        when(fingerprintService.computeFingerprint(any(), any(), any(), any())).thenReturn("hash54321");
        when(episodeRepository.findByFingerprint("hash54321")).thenReturn(Optional.empty());

        when(episodeRepository.save(any(Episode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sceneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Episode result = episodeScriptService.generateAndSaveEpisode(seriesId, request);

        assertThat(result.getStatus()).isEqualTo(EpisodeStatus.AWAITING_SCRIPT_APPROVAL);
    }

    @Test
    void rejectsDuplicateFingerprint() throws Exception {
        CreateEpisodeRequest request = new CreateEpisodeRequest(
                EducationalTopicType.COUNTING,
                "Contar del 1 al 5",
                "Reconocer números del 1 al 5",
                120,
                true
        );

        EpisodeScript script = createSampleScript();
        String json = objectMapper.writeValueAsString(script);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(llmProvider.getProviderName()).thenReturn("ollama-qwen2.5");
        when(promptTemplateService.getSystemPrompt()).thenReturn("System prompt");
        when(promptTemplateService.buildUserPrompt(any(), any(), any(), any(), anyInt())).thenReturn("User prompt");
        when(promptTemplateService.getEpisodeScriptSchema()).thenReturn(Map.of("type", "object"));
        when(llmProvider.generateStructured(anyString(), anyString(), any())).thenReturn(json);

        when(schemaValidator.validate(any(), any(), any())).thenReturn(List.of());
        when(contentSafetyService.evaluate(any(), anyBoolean())).thenReturn(PedagogicalReviewResult.ok());
        when(fingerprintService.computeFingerprint(any(), any(), any(), any())).thenReturn("duplicate_hash");

        Episode existingEpisode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "obj", "titulo existente");
        when(episodeRepository.findByFingerprint("duplicate_hash")).thenReturn(Optional.of(existingEpisode));

        assertThatThrownBy(() -> episodeScriptService.generateAndSaveEpisode(seriesId, request))
                .isInstanceOf(ScriptValidationException.class)
                .hasMessageContaining("Episodio duplicado detectado");
    }

    @Test
    void retriesWhenInitialAttemptFailsPedagogicalValidation() throws Exception {
        CreateEpisodeRequest request = new CreateEpisodeRequest(
                EducationalTopicType.COUNTING,
                "Contar del 1 al 5",
                "Reconocer números del 1 al 5",
                120,
                true
        );

        EpisodeScript script = createSampleScript();
        String json = objectMapper.writeValueAsString(script);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(llmProvider.getProviderName()).thenReturn("ollama-qwen2.5");
        when(promptTemplateService.getSystemPrompt()).thenReturn("System prompt");
        when(promptTemplateService.buildUserPrompt(any(), any(), any(), any(), anyInt())).thenReturn("User prompt");
        when(promptTemplateService.getEpisodeScriptSchema()).thenReturn(Map.of("type", "object"));
        when(llmProvider.generateStructured(anyString(), anyString(), any())).thenReturn(json);

        // First attempt schema passes, but safety fails. Second attempt safety passes.
        when(schemaValidator.validate(any(), any(), any())).thenReturn(List.of());
        when(contentSafetyService.evaluate(any(), anyBoolean()))
                .thenReturn(PedagogicalReviewResult.rejected(List.of("Palabra compleja para niños")))
                .thenReturn(PedagogicalReviewResult.ok());

        when(fingerprintService.computeFingerprint(any(), any(), any(), any())).thenReturn("hash_retry");
        when(episodeRepository.findByFingerprint("hash_retry")).thenReturn(Optional.empty());
        when(episodeRepository.save(any(Episode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sceneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Episode result = episodeScriptService.generateAndSaveEpisode(seriesId, request);

        assertThat(result).isNotNull();
        verify(llmProvider, times(2)).generateStructured(anyString(), anyString(), any());
    }

    @Test
    void approvesScriptManually() {
        UUID episodeId = UUID.randomUUID();
        Episode ep = new Episode(series, EducationalTopicType.COUNTING, "Contar", "obj", "¡Hola!");
        ep.setStatus(EpisodeStatus.AWAITING_SCRIPT_APPROVAL);

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(ep));
        when(episodeRepository.save(any(Episode.class))).thenAnswer(inv -> inv.getArgument(0));

        Episode approved = episodeScriptService.approveScript(episodeId);

        assertThat(approved.getStatus()).isEqualTo(EpisodeStatus.NARRATION);
    }
}
