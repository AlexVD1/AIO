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
import com.kidsanim.api.dto.ShotAnimationResponse;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.gateway.AiGatewayClient.GpuFreeResponse;
import com.kidsanim.api.gateway.AiGatewayClient.VideoI2VAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.VideoI2VAiResponse;
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
class AnimationStageServiceTest {

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
    private AnimationStageService animationStageService;

    private Series series;
    private Episode episode;
    private Scene scene;
    private Shot shot1;
    private Shot shot2;
    private File dummyKeyframeFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        pipelineProperties = new KidsPipelineProperties(tempDir.toString(), "ef_dora", "-12%", 0.80, 3);

        animationStageService = new AnimationStageService(
                episodeRepository,
                sceneRepository,
                shotRepository,
                assetRepository,
                pipelineJobRepository,
                aiGatewayClient,
                pipelineProperties,
                objectMapper
        );

        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        styleProfile.setVideoResolution("768x512");
        styleProfile.setVideoFps(16);
        series = new Series("Tito el zorrito", "Preescolar", styleProfile);

        Character tito = new Character(series, "Tito", CharacterRole.HOST, "cute orange cartoon fox");
        series.getCharacters().add(tito);

        episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Números", "Episodio 1");
        episode.setStatus(EpisodeStatus.ANIMATION);
        ReflectionTestUtils.setField(episode, "id", UUID.randomUUID());

        scene = new Scene(episode, 1, new Location(series, "Huerto", "huerto"), com.kidsanim.api.domain.enums.ScenePurpose.INTRO, "playful");
        ReflectionTestUtils.setField(scene, "id", UUID.randomUUID());

        shot1 = new Shot(scene, 1, "¡Hola amiguitos!", "waving happily");
        shot1.setCharacter(tito);
        shot1.setActionPrompt("the fox waves its paw and smiles");
        shot1.setContinuityMode(ContinuityMode.NEW_KEYFRAME);
        shot1.setTargetDurationMs(3000);
        shot1.setStatus(ShotStatus.KEYFRAME_READY);
        ReflectionTestUtils.setField(shot1, "id", UUID.randomUUID());

        shot2 = new Shot(scene, 2, "Una manzana", "looking at apple");
        shot2.setCharacter(tito);
        shot2.setActionPrompt("the apple gently sways");
        shot2.setContinuityMode(ContinuityMode.CHAIN_LAST_FRAME);
        shot2.setTargetDurationMs(4000);
        shot2.setStatus(ShotStatus.KEYFRAME_READY);
        ReflectionTestUtils.setField(shot2, "id", UUID.randomUUID());

        scene.getShots().add(shot1);
        scene.getShots().add(shot2);
        episode.getScenes().add(scene);

        dummyKeyframeFile = tempDir.resolve("keyframe_1_1.png").toFile();
        dummyKeyframeFile.createNewFile();

        lenient().when(assetRepository.findByShotIdAndType(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void processesEpisodeAnimationWithChainLastFrameAndAssetCreation() {
        UUID episodeId = episode.getId();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(pipelineJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Asset keyframe for shot1
        Asset keyframeAsset = new Asset(episode, shot1, AssetType.KEYFRAME, dummyKeyframeFile.getAbsolutePath());
        when(assetRepository.findByShotIdAndType(shot1.getId(), AssetType.KEYFRAME)).thenReturn(Optional.of(keyframeAsset));

        when(aiGatewayClient.generateVideoI2V(any())).thenAnswer(inv -> {
            VideoI2VAiRequest req = inv.getArgument(0);
            String lastFrame = req.outputPath().replace(".mp4", "_last_frame.png");
            // Crear el archivo para simular que existe en disco para la siguiente pasada
            try {
                new File(lastFrame).createNewFile();
            } catch (Exception ignored) {}
            return new VideoI2VAiResponse(req.outputPath(), lastFrame, req.fps() * (req.durationMs() / 1000), req.durationMs());
        });

        when(aiGatewayClient.freeGpu()).thenReturn(new GpuFreeResponse(2048));

        Episode result = animationStageService.processEpisodeAnimation(episodeId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EpisodeStatus.SUBTITLES_OVERLAYS);

        // Verify shots marked as ANIMATED
        assertThat(shot1.getStatus()).isEqualTo(ShotStatus.ANIMATED);
        assertThat(shot2.getStatus()).isEqualTo(ShotStatus.ANIMATED);

        // Verify VideoI2V requests
        ArgumentCaptor<VideoI2VAiRequest> captor = ArgumentCaptor.forClass(VideoI2VAiRequest.class);
        verify(aiGatewayClient, times(2)).generateVideoI2V(captor.capture());
        List<VideoI2VAiRequest> requests = captor.getAllValues();

        // Shot 1 used dummy keyframe
        assertThat(requests.get(0).imagePath()).isEqualTo(dummyKeyframeFile.getAbsolutePath());
        assertThat(requests.get(0).actionPrompt()).isEqualTo("the fox waves its paw and smiles");
        assertThat(requests.get(0).durationMs()).isEqualTo(3000);

        // Shot 2 (CHAIN_LAST_FRAME) used the last frame from shot 1
        assertThat(requests.get(1).imagePath()).contains("_last_frame.png");
        assertThat(requests.get(1).actionPrompt()).isEqualTo("the apple gently sways");
        assertThat(requests.get(1).durationMs()).isEqualTo(4000);

        // Verify assets saved: 2 shots * 2 assets (CLIP + CLIP_LAST_FRAME) = 4 assets saved
        verify(assetRepository, times(4)).save(any(Asset.class));

        // GPU free called after stage
        verify(aiGatewayClient, times(1)).freeGpu();
    }

    @Test
    void retriesOnFailureAndFallsBackToDegradedIfExhausted() {
        UUID episodeId = episode.getId();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(pipelineJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Asset keyframeAsset = new Asset(episode, shot1, AssetType.KEYFRAME, dummyKeyframeFile.getAbsolutePath());
        when(assetRepository.findByShotIdAndType(shot1.getId(), AssetType.KEYFRAME)).thenReturn(Optional.of(keyframeAsset));

        // Gateway falla persistentemente
        when(aiGatewayClient.generateVideoI2V(any())).thenThrow(new RuntimeException("GPU Out of Memory"));
        when(aiGatewayClient.freeGpu()).thenReturn(new GpuFreeResponse(0));

        animationStageService.processEpisodeAnimation(episodeId);

        // Shot 1 debe haber reintentado 2 veces y quedar marcado como DEGRADED
        assertThat(shot1.getStatus()).isEqualTo(ShotStatus.DEGRADED);
        assertThat(shot1.getAttempts()).isEqualTo(2);

        // 2 intentos para shot 1 (y shot 2 falla o se marca al no tener frame anterior)
        verify(aiGatewayClient, atLeast(2)).generateVideoI2V(any());
        verify(aiGatewayClient, times(1)).freeGpu();
    }

    @Test
    void regeneratesSingleShotAnimation() {
        UUID shotId = shot1.getId();
        when(shotRepository.findById(shotId)).thenReturn(Optional.of(shot1));
        when(shotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Asset keyframeAsset = new Asset(episode, shot1, AssetType.KEYFRAME, dummyKeyframeFile.getAbsolutePath());
        when(assetRepository.findByShotIdAndType(shotId, AssetType.KEYFRAME)).thenReturn(Optional.of(keyframeAsset));

        when(aiGatewayClient.generateVideoI2V(any())).thenReturn(
                new VideoI2VAiResponse("/storage/clips/regenerated.mp4", "/storage/clips/regenerated_last.png", 48, 3000)
        );
        when(aiGatewayClient.freeGpu()).thenReturn(new GpuFreeResponse(100));

        Shot regenerated = animationStageService.regenerateShotAnimation(shotId);

        assertThat(regenerated).isNotNull();
        assertThat(regenerated.getStatus()).isEqualTo(ShotStatus.ANIMATED);

        verify(aiGatewayClient, times(1)).freeGpu();
    }

    @Test
    void getsShotAnimationResponseDto() {
        UUID shotId = shot1.getId();
        shot1.setStatus(ShotStatus.ANIMATED);
        shot1.setSeed(777L);
        shot1.setAttempts(1);

        when(shotRepository.findById(shotId)).thenReturn(Optional.of(shot1));

        Asset clipAsset = new Asset(episode, shot1, AssetType.CLIP, "/path/to/clip.mp4");
        clipAsset.setMetaJson("{\"frames\":48,\"durationMs\":3000,\"actionPrompt\":\"fox dancing\"}");
        when(assetRepository.findByShotIdAndType(shotId, AssetType.CLIP)).thenReturn(Optional.of(clipAsset));

        Asset lastFrameAsset = new Asset(episode, shot1, AssetType.CLIP_LAST_FRAME, "/path/to/last_frame.png");
        when(assetRepository.findByShotIdAndType(shotId, AssetType.CLIP_LAST_FRAME)).thenReturn(Optional.of(lastFrameAsset));

        ShotAnimationResponse response = animationStageService.getShotAnimation(shotId);

        assertThat(response).isNotNull();
        assertThat(response.shotId()).isEqualTo(shotId);
        assertThat(response.status()).isEqualTo(ShotStatus.ANIMATED);
        assertThat(response.clipPath()).isEqualTo("/path/to/clip.mp4");
        assertThat(response.lastFramePath()).isEqualTo("/path/to/last_frame.png");
        assertThat(response.frames()).isEqualTo(48);
        assertThat(response.durationMs()).isEqualTo(3000);
        assertThat(response.actionPrompt()).isEqualTo("fox dancing");
    }
}
