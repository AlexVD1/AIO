package com.kidsanim.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.PipelineJob;
import com.kidsanim.api.domain.Scene;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.PipelineJobStatus;
import com.kidsanim.api.domain.enums.ShotStatus;
import com.kidsanim.api.dto.ShotNarrationResponse;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.gateway.AiGatewayClient.AlignAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.AlignAiResponse;
import com.kidsanim.api.gateway.AiGatewayClient.TtsAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.TtsAiResponse;
import com.kidsanim.api.gateway.AiGatewayClient.WordTimestamp;
import com.kidsanim.api.infrastructure.config.KidsPipelineProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import com.kidsanim.api.repository.SceneRepository;
import com.kidsanim.api.repository.ShotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NarrationStageServiceTest {

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private SceneRepository sceneRepository;

    @Mock
    private ShotRepository shotRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PipelineJobRepository pipelineJobRepository;

    @Mock
    private AiGatewayClient aiGatewayClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KidsPipelineProperties pipelineProperties;
    private NarrationStageService narrationStageService;

    private Series series;
    private Episode episode;
    private Scene scene;
    private Shot shot1;
    private Shot shot2;

    @BeforeEach
    void setUp() {
        pipelineProperties = new KidsPipelineProperties("./storage", "ef_dora", "-12%");

        narrationStageService = new NarrationStageService(
                episodeRepository,
                sceneRepository,
                shotRepository,
                assetRepository,
                pipelineJobRepository,
                aiGatewayClient,
                pipelineProperties,
                objectMapper
        );

        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad");
        series = new Series("Tito el zorrito", "Preescolar", styleProfile);

        Character tito = new Character(series, "Tito", CharacterRole.HOST, "cute fox");
        tito.setTtsVoice("em_alex");
        tito.setTtsRate("-10%");
        series.getCharacters().add(tito);

        episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Números", "Episodio 1");
        episode.setStatus(EpisodeStatus.NARRATION);
        org.springframework.test.util.ReflectionTestUtils.setField(episode, "id", UUID.randomUUID());

        scene = new Scene(episode, 1, new Location(series, "Huerto", "huerto"), com.kidsanim.api.domain.enums.ScenePurpose.INTRO, "playful");
        org.springframework.test.util.ReflectionTestUtils.setField(scene, "id", UUID.randomUUID());

        shot1 = new Shot(scene, 1, "¡Hola amiguitos!", "Tito waving");
        shot1.setCharacter(tito);
        shot1.setSpeaker("Tito");
        shot1.setPauseAfterMs(300);
        org.springframework.test.util.ReflectionTestUtils.setField(shot1, "id", UUID.randomUUID());

        shot2 = new Shot(scene, 2, "¿Cuántas manzanas hay?", "Tree with apples");
        shot2.setCharacter(null);
        shot2.setSpeaker("NARRATOR");
        shot2.setPauseAfterMs(500);
        org.springframework.test.util.ReflectionTestUtils.setField(shot2, "id", UUID.randomUUID());

        scene.getShots().add(shot1);
        scene.getShots().add(shot2);
        episode.getScenes().add(scene);
    }

    @Test
    void processesEpisodeNarrationWithDistinctVoicesAndCalculatesDuration() {
        UUID episodeId = UUID.randomUUID();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(pipelineJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.findByShotIdAndType(any(), any())).thenReturn(Optional.empty());

        // Mock TTS returns
        when(aiGatewayClient.generateTts(any())).thenAnswer(inv -> {
            TtsAiRequest req = inv.getArgument(0);
            return new TtsAiResponse(req.outputPath(), 2800);
        });

        // Mock Align returns
        when(aiGatewayClient.alignAudio(any())).thenReturn(new AlignAiResponse(List.of(
                new WordTimestamp("Hola", 0, 400),
                new WordTimestamp("amiguitos", 420, 1100)
        )));

        Episode result = narrationStageService.processEpisodeNarration(episodeId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EpisodeStatus.KEYFRAMES);

        // Verify distinct voices called
        ArgumentCaptor<TtsAiRequest> captor = ArgumentCaptor.forClass(TtsAiRequest.class);
        verify(aiGatewayClient, times(2)).generateTts(captor.capture());
        List<TtsAiRequest> requests = captor.getAllValues();

        // Shot 1 used Tito's voice
        assertThat(requests.get(0).voice()).isEqualTo("em_alex");
        assertThat(requests.get(0).rate()).isEqualTo("-10%");

        // Shot 2 used default narrator voice
        assertThat(requests.get(1).voice()).isEqualTo("ef_dora");
        assertThat(requests.get(1).rate()).isEqualTo("-12%");

        // Verify target duration logic (minimum 3000 ms)
        // audioDuration = 2800, pauseAfterMs = 300 -> 3100 ms
        assertThat(shot1.getTargetDurationMs()).isEqualTo(3100);
        assertThat(shot1.getStatus()).isEqualTo(ShotStatus.NARRATED);

        // Shot 2: audioDuration = 2800, pauseAfterMs = 500 -> 3300 ms
        assertThat(shot2.getTargetDurationMs()).isEqualTo(3300);
        assertThat(shot2.getStatus()).isEqualTo(ShotStatus.NARRATED);

        // Verify assets created: 2 shots * 2 assets (AUDIO + TIMESTAMPS) = 4 assets
        verify(assetRepository, times(4)).save(any(Asset.class));
    }

    @Test
    void ensuresMinimumThreeSecondsTargetDuration() {
        UUID episodeId = UUID.randomUUID();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(pipelineJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.findByShotIdAndType(any(), any())).thenReturn(Optional.empty());

        // Short audio: only 1000 ms + 300 pause = 1300 ms, should be clamped to 3000 ms
        when(aiGatewayClient.generateTts(any())).thenAnswer(inv -> {
            TtsAiRequest req = inv.getArgument(0);
            return new TtsAiResponse(req.outputPath(), 1000);
        });
        when(aiGatewayClient.alignAudio(any())).thenReturn(new AlignAiResponse(List.of()));

        narrationStageService.processEpisodeNarration(episodeId);

        assertThat(shot1.getTargetDurationMs()).isEqualTo(3000);
    }

    @Test
    void regeneratesSingleShotNarration() {
        UUID shotId = UUID.randomUUID();
        when(shotRepository.findById(shotId)).thenReturn(Optional.of(shot1));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.findByShotIdAndType(any(), any())).thenReturn(Optional.empty());

        when(aiGatewayClient.generateTts(any())).thenAnswer(inv -> {
            TtsAiRequest req = inv.getArgument(0);
            return new TtsAiResponse(req.outputPath(), 2500);
        });
        when(aiGatewayClient.alignAudio(any())).thenReturn(new AlignAiResponse(List.of(
                new WordTimestamp("Hola", 0, 500)
        )));

        Shot regenerated = narrationStageService.regenerateShotNarration(shotId);

        assertThat(regenerated).isNotNull();
        assertThat(regenerated.getStatus()).isEqualTo(ShotStatus.NARRATED);
        assertThat(regenerated.getTargetDurationMs()).isEqualTo(3000); // 2500 + 300 = 2800 -> clamped to 3000
    }

    @Test
    void generatesReadingScriptSuccessfully() {
        when(episodeRepository.findById(episode.getId())).thenReturn(Optional.of(episode));
        when(sceneRepository.findByEpisodeIdOrderByOrderIndexAsc(episode.getId())).thenReturn(List.of(scene));
        when(shotRepository.findBySceneIdOrderByOrderIndexAsc(scene.getId())).thenReturn(List.of(shot1, shot2));

        String script = narrationStageService.generateReadingScript(episode.getId());

        assertThat(script).contains("GUION DE LECTURA");
        assertThat(script).contains("Episodio 1");
        assertThat(script).contains("¡Hola amiguitos!");
    }

    @Test
    void processesRecordedAudioUploadSuccessfully() {
        when(episodeRepository.findById(episode.getId())).thenReturn(Optional.of(episode));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sceneRepository.findByEpisodeIdOrderByOrderIndexAsc(episode.getId())).thenReturn(List.of(scene));
        when(shotRepository.findBySceneIdOrderByOrderIndexAsc(scene.getId())).thenReturn(List.of(shot1, shot2));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(aiGatewayClient.alignAudio(any())).thenReturn(new AlignAiResponse(List.of(
                new WordTimestamp("Hola", 0, 500),
                new WordTimestamp("niños", 500, 1000)
        )));

        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
        Episode processed = narrationStageService.processRecordedAudioUpload(episode.getId(), bais, "test.wav");

        assertThat(processed.getStatus()).isEqualTo(EpisodeStatus.KEYFRAMES);
        assertThat(shot1.getStatus()).isEqualTo(ShotStatus.NARRATED);
    }
}
