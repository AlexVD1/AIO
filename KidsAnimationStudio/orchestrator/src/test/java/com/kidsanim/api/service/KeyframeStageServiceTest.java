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
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.ShotStatus;
import com.kidsanim.api.dto.ShotKeyframeResponse;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.gateway.AiGatewayClient.GpuFreeResponse;
import com.kidsanim.api.gateway.AiGatewayClient.KeyframeAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.KeyframeAiResponse;
import com.kidsanim.api.gateway.AiGatewayClient.SimilarityAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.SimilarityAiResponse;
import com.kidsanim.api.infrastructure.config.KidsPipelineProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import com.kidsanim.api.repository.SceneRepository;
import com.kidsanim.api.repository.ShotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyframeStageServiceTest {

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
    private KeyframeStageService keyframeStageService;

    private Series series;
    private Episode episode;
    private Scene scene;
    private Shot shot1;
    private Shot shot2;
    private Character tito;
    private File dummyRefImage;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        pipelineProperties = new KidsPipelineProperties(tempDir.toString(), "ef_dora", "-12%", 0.80, 3);

        keyframeStageService = new KeyframeStageService(
                episodeRepository,
                sceneRepository,
                shotRepository,
                assetRepository,
                pipelineJobRepository,
                aiGatewayClient,
                pipelineProperties,
                objectMapper
        );

        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon style, pixar render", "bad quality, deformed");
        series = new Series("Tito el zorrito", "Preescolar", styleProfile);

        dummyRefImage = tempDir.resolve("tito_ref.png").toFile();
        dummyRefImage.createNewFile();

        tito = new Character(series, "Tito", CharacterRole.HOST, "cute orange cartoon fox wearing blue shirt");
        tito.setReferenceImagePath(dummyRefImage.getAbsolutePath());
        tito.setReferenceSeed(42L);
        tito.setIpadapterWeight(new BigDecimal("0.85"));
        series.getCharacters().add(tito);

        episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Números", "Episodio 1");
        episode.setStatus(EpisodeStatus.KEYFRAMES);
        ReflectionTestUtils.setField(episode, "id", UUID.randomUUID());

        scene = new Scene(episode, 1, new Location(series, "Huerto", "sunny apple orchard"), com.kidsanim.api.domain.enums.ScenePurpose.INTRO, "playful");
        ReflectionTestUtils.setField(scene, "id", UUID.randomUUID());

        shot1 = new Shot(scene, 1, "¡Hola amiguitos!", "waving happily at camera");
        shot1.setCharacter(tito);
        shot1.setContinuityMode(ContinuityMode.NEW_KEYFRAME);
        ReflectionTestUtils.setField(shot1, "id", UUID.randomUUID());

        shot2 = new Shot(scene, 2, "Una manzana", "looking at apple");
        shot2.setCharacter(tito);
        shot2.setContinuityMode(ContinuityMode.CHAIN_LAST_FRAME);
        ReflectionTestUtils.setField(shot2, "id", UUID.randomUUID());

        scene.getShots().add(shot1);
        scene.getShots().add(shot2);
        episode.getScenes().add(scene);
    }

    @Test
    void processesEpisodeKeyframesWithIpAdapterAndChainLastFrame() {
        UUID episodeId = episode.getId();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(pipelineJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.findByShotIdAndType(any(), eq(AssetType.KEYFRAME))).thenReturn(Optional.empty());

        // Shot 1: AI gateway returns generated image
        when(aiGatewayClient.generateKeyframe(any())).thenAnswer(inv -> {
            KeyframeAiRequest req = inv.getArgument(0);
            return new KeyframeAiResponse(req.outputPath(), req.seed());
        });

        // Similarity check returns 0.88 (> 0.80 threshold)
        when(aiGatewayClient.calculateSimilarity(any())).thenReturn(new SimilarityAiResponse(0.88));
        when(aiGatewayClient.freeGpu()).thenReturn(new GpuFreeResponse(1200));

        Episode result = keyframeStageService.processEpisodeKeyframes(episodeId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EpisodeStatus.ANIMATION);

        // Verify Shot 1 is KEYFRAME_READY with similarity 0.88
        assertThat(shot1.getStatus()).isEqualTo(ShotStatus.KEYFRAME_READY);
        assertThat(shot1.getSimilarityScore()).isEqualByComparingTo("0.88");
        assertThat(shot1.getAttempts()).isEqualTo(1);
        assertThat(shot1.getSeed()).isNotNull();

        // Verify Shot 2 (CHAIN_LAST_FRAME) skipped generation and is KEYFRAME_READY
        assertThat(shot2.getStatus()).isEqualTo(ShotStatus.KEYFRAME_READY);
        assertThat(shot2.getAttempts()).isEqualTo(0);

        // AI gateway generateKeyframe called only ONCE (for Shot 1, Shot 2 skipped)
        verify(aiGatewayClient, times(1)).generateKeyframe(any());

        // GPU free called after stage completion
        verify(aiGatewayClient, times(1)).freeGpu();
    }

    @Test
    void retriesUpToMaxAttemptsAndMarksDegradedWhenBelowThreshold() {
        UUID episodeId = episode.getId();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(pipelineJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.findByShotIdAndType(any(), eq(AssetType.KEYFRAME))).thenReturn(Optional.empty());

        when(aiGatewayClient.generateKeyframe(any())).thenAnswer(inv -> {
            KeyframeAiRequest req = inv.getArgument(0);
            return new KeyframeAiResponse(req.outputPath(), req.seed());
        });

        // Similitud baja persistente: 0.65 (< 0.80)
        when(aiGatewayClient.calculateSimilarity(any())).thenReturn(new SimilarityAiResponse(0.65));
        when(aiGatewayClient.freeGpu()).thenReturn(new GpuFreeResponse(0));

        keyframeStageService.processEpisodeKeyframes(episodeId);

        // Shot 1 debe haber reintentado 3 veces y quedar marcado como DEGRADED
        assertThat(shot1.getStatus()).isEqualTo(ShotStatus.DEGRADED);
        assertThat(shot1.getAttempts()).isEqualTo(3);
        assertThat(shot1.getSimilarityScore()).isEqualByComparingTo("0.65");

        // 3 llamadas para shot 1 (y 0 para shot 2)
        verify(aiGatewayClient, times(3)).generateKeyframe(any());
        verify(aiGatewayClient, times(3)).calculateSimilarity(any());
    }

    @Test
    void regeneratesSingleShotKeyframe() {
        UUID shotId = shot1.getId();
        when(shotRepository.findById(shotId)).thenReturn(Optional.of(shot1));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.findByShotIdAndType(shotId, AssetType.KEYFRAME)).thenReturn(Optional.empty());

        when(aiGatewayClient.generateKeyframe(any())).thenAnswer(inv -> {
            KeyframeAiRequest req = inv.getArgument(0);
            return new KeyframeAiResponse(req.outputPath(), 9999L);
        });
        when(aiGatewayClient.calculateSimilarity(any())).thenReturn(new SimilarityAiResponse(0.92));
        when(aiGatewayClient.freeGpu()).thenReturn(new GpuFreeResponse(500));

        Shot regenerated = keyframeStageService.regenerateShotKeyframe(shotId);

        assertThat(regenerated).isNotNull();
        assertThat(regenerated.getStatus()).isEqualTo(ShotStatus.KEYFRAME_READY);
        assertThat(regenerated.getSimilarityScore()).isEqualByComparingTo("0.92");
        assertThat(regenerated.getSeed()).isEqualTo(9999L);

        verify(aiGatewayClient, times(1)).freeGpu();
    }

    @Test
    void getsShotKeyframeResponseDto() {
        UUID shotId = shot1.getId();
        shot1.setStatus(ShotStatus.KEYFRAME_READY);
        shot1.setSimilarityScore(new BigDecimal("0.875"));
        shot1.setSeed(12345L);
        shot1.setAttempts(1);

        when(shotRepository.findById(shotId)).thenReturn(Optional.of(shot1));

        Asset keyframeAsset = new Asset(episode, shot1, AssetType.KEYFRAME, "/path/to/keyframe.png");
        keyframeAsset.setMetaJson("{\"prompt\":\"a cute fox\",\"similarityScore\":0.875}");
        when(assetRepository.findByShotIdAndType(shotId, AssetType.KEYFRAME)).thenReturn(Optional.of(keyframeAsset));

        ShotKeyframeResponse response = keyframeStageService.getShotKeyframe(shotId);

        assertThat(response).isNotNull();
        assertThat(response.shotId()).isEqualTo(shotId);
        assertThat(response.status()).isEqualTo(ShotStatus.KEYFRAME_READY);
        assertThat(response.keyframePath()).isEqualTo("/path/to/keyframe.png");
        assertThat(response.similarityScore()).isEqualTo(0.875);
        assertThat(response.prompt()).isEqualTo("a cute fox");
        assertThat(response.characterName()).isEqualTo("Tito");
    }
}
