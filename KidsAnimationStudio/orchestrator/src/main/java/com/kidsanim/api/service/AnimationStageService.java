package com.kidsanim.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.PipelineJob;
import com.kidsanim.api.domain.Scene;
import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.PipelineJobStatus;
import com.kidsanim.api.domain.enums.PipelineStage;
import com.kidsanim.api.domain.enums.ShotStatus;
import com.kidsanim.api.dto.ShotAnimationResponse;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.gateway.AiGatewayClient.VideoI2VAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.VideoI2VAiResponse;
import com.kidsanim.api.infrastructure.config.KidsPipelineProperties;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import com.kidsanim.api.repository.SceneRepository;
import com.kidsanim.api.repository.ShotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AnimationStageService {

    private static final Logger log = LoggerFactory.getLogger(AnimationStageService.class);

    private final EpisodeRepository episodeRepository;
    private final SceneRepository sceneRepository;
    private final ShotRepository shotRepository;
    private final AssetRepository assetRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final AiGatewayClient aiGatewayClient;
    private final KidsPipelineProperties pipelineProperties;
    private final ObjectMapper objectMapper;

    public AnimationStageService(EpisodeRepository episodeRepository,
                                 SceneRepository sceneRepository,
                                 ShotRepository shotRepository,
                                 AssetRepository assetRepository,
                                 PipelineJobRepository pipelineJobRepository,
                                 AiGatewayClient aiGatewayClient,
                                 KidsPipelineProperties pipelineProperties,
                                 ObjectMapper objectMapper) {
        this.episodeRepository = episodeRepository;
        this.sceneRepository = sceneRepository;
        this.shotRepository = shotRepository;
        this.assetRepository = assetRepository;
        this.pipelineJobRepository = pipelineJobRepository;
        this.aiGatewayClient = aiGatewayClient;
        this.pipelineProperties = pipelineProperties;
        this.objectMapper = objectMapper;
    }

    public Episode processEpisodeAnimation(UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        log.info("Iniciando etapa ANIMATION para el episodio '{}' ({})", episode.getTitle(), episode.getId());

        PipelineJob job = new PipelineJob(episode, PipelineStage.ANIMATION);
        job.setStatus(PipelineJobStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        job.setProgressPercent(0);
        job = pipelineJobRepository.save(job);

        List<Scene> scenes = episode.getScenes();
        if (scenes == null || scenes.isEmpty()) {
            scenes = sceneRepository.findByEpisodeIdOrderByOrderIndexAsc(episodeId);
        } else {
            scenes.sort(Comparator.comparingInt(Scene::getOrderIndex));
        }

        List<Shot> allShots = new ArrayList<>();
        for (Scene scene : scenes) {
            List<Shot> shots = scene.getShots();
            if (shots == null || shots.isEmpty()) {
                shots = shotRepository.findBySceneIdOrderByOrderIndexAsc(scene.getId());
            } else {
                shots.sort(Comparator.comparingInt(Shot::getOrderIndex));
            }
            allShots.addAll(shots);
        }

        int totalShots = allShots.size();
        int completedShots = 0;
        String lastFrameOfPreviousShot = null;

        try {
            for (Shot shot : allShots) {
                // Idempotencia: si ya tiene CLIP y CLIP_LAST_FRAME válidos en disco, reutilizarlos
                if (shot.getStatus() == ShotStatus.ANIMATED || shot.getStatus() == ShotStatus.DEGRADED) {
                    var existingClip = assetRepository.findByShotIdAndType(shot.getId(), AssetType.CLIP);
                    var existingLastFrame = assetRepository.findByShotIdAndType(shot.getId(), AssetType.CLIP_LAST_FRAME);
                    if (existingClip.isPresent() && new File(existingClip.get().getPath()).exists()
                            && existingLastFrame.isPresent() && new File(existingLastFrame.get().getPath()).exists()) {
                        log.info("Plano {} ya cuenta con clip y último frame válidos, saltando...", shot.getId());
                        lastFrameOfPreviousShot = existingLastFrame.get().getPath();
                        completedShots++;
                        continue;
                    }
                }

                ShotAnimationResult animResult = animateSingleShot(episode, shot, lastFrameOfPreviousShot);
                if (animResult.lastFramePath() != null) {
                    lastFrameOfPreviousShot = animResult.lastFramePath();
                }

                completedShots++;
                int progress = (int) Math.round(((double) completedShots / Math.max(1, totalShots)) * 100.0);
                job.setProgressPercent(progress);
                pipelineJobRepository.save(job);
            }
        } finally {
            // Liberar memoria VRAM en AI Gateway tras completar o ante error
            try {
                aiGatewayClient.freeGpu();
            } catch (Exception ex) {
                log.warn("No se pudo liberar VRAM tras etapa ANIMATION: {}", ex.getMessage());
            }
        }

        job.setStatus(PipelineJobStatus.COMPLETED);
        job.setProgressPercent(100);
        job.setFinishedAt(OffsetDateTime.now());
        pipelineJobRepository.save(job);

        episode.setStatus(EpisodeStatus.SUBTITLES_OVERLAYS);
        Episode savedEpisode = episodeRepository.save(episode);

        log.info("Etapa ANIMATION completada para episodio '{}'. Total planos animados: {}. Nuevo estado: {}",
                savedEpisode.getTitle(), totalShots, savedEpisode.getStatus());

        return savedEpisode;
    }

    public record ShotAnimationResult(Shot shot, String clipPath, String lastFramePath) {}

    public ShotAnimationResult animateSingleShot(Episode episode, Shot shot, String previousLastFramePath) {
        Scene scene = shot.getScene();
        int sceneOrder = scene != null ? scene.getOrderIndex() : 1;
        int shotOrder = shot.getOrderIndex();

        // 1. Determinar imagen de entrada según continuityMode
        String inputImagePath = null;
        if (shot.getContinuityMode() == ContinuityMode.CHAIN_LAST_FRAME && previousLastFramePath != null && new File(previousLastFramePath).exists()) {
            inputImagePath = previousLastFramePath;
            log.info("Plano {} usando lastFramePath del plano anterior para encadenamiento: {}", shot.getId(), inputImagePath);
        } else {
            var keyframeAssetOpt = assetRepository.findByShotIdAndType(shot.getId(), AssetType.KEYFRAME);
            if (keyframeAssetOpt.isPresent() && new File(keyframeAssetOpt.get().getPath()).exists()) {
                inputImagePath = keyframeAssetOpt.get().getPath();
            } else if (previousLastFramePath != null && new File(previousLastFramePath).exists()) {
                inputImagePath = previousLastFramePath;
            }
        }

        if (inputImagePath == null) {
            log.warn("No se encontró keyframe ni frame anterior para animar plano {}. Marcando DEGRADED.", shot.getId());
            shot.setStatus(ShotStatus.DEGRADED);
            Shot saved = shotRepository.save(shot);
            return new ShotAnimationResult(saved, null, null);
        }

        // 2. Parámetros del modelo y estilo
        StyleProfile style = episode.getSeries() != null ? episode.getSeries().getStyleProfile() : null;
        String model = (style != null && style.getVideoModel() != null) ? style.getVideoModel().name() : "LTX";
        int fps = (style != null && style.getVideoFps() > 0) ? style.getVideoFps() : 24;
        int width = 1024;
        int height = 576;
        if (style != null && style.getVideoResolution() != null && style.getVideoResolution().contains("x")) {
            try {
                String[] parts = style.getVideoResolution().trim().split("x");
                width = Integer.parseInt(parts[0]);
                height = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }

        String actionPrompt = (shot.getActionPrompt() != null && !shot.getActionPrompt().isBlank())
                ? shot.getActionPrompt()
                : (shot.getVisualPrompt() != null ? shot.getVisualPrompt() : "gentle cartoon movement");

        String negativePrompt = (style != null && style.getNegativePrompt() != null && !style.getNegativePrompt().isBlank())
                ? style.getNegativePrompt()
                : "worst quality, distorted, jitter, static, blur";

        int durationMs = shot.getTargetDurationMs() > 0 ? shot.getTargetDurationMs() : 4000;

        String episodeDirName = episode.getId() != null ? episode.getId().toString() : "ep_temp";
        Path clipsDir = Paths.get(pipelineProperties.storagePath(), "episodes", episodeDirName, "clips");
        clipsDir.toFile().mkdirs();

        // 3. Ejecución I2V con reintentos (hasta 2 intentos)
        int maxAttempts = 2;
        int attemptUsed = 1;
        VideoI2VAiResponse videoResponse = null;
        Long baseSeed = shot.getSeed() != null ? shot.getSeed() : (long) (Math.random() * 1_000_000_000L);
        Long usedSeed = baseSeed;

        for (int a = 1; a <= maxAttempts; a++) {
            attemptUsed = a;
            usedSeed = baseSeed + (a - 1) * 37L;
            Path outClip = clipsDir.resolve(String.format("shot_%d_%d_attempt_%d.mp4", sceneOrder, shotOrder, a));

            VideoI2VAiRequest req = new VideoI2VAiRequest(
                    model,
                    inputImagePath,
                    actionPrompt,
                    negativePrompt,
                    durationMs,
                    fps,
                    width,
                    height,
                    usedSeed,
                    outClip.toAbsolutePath().toString()
            );

            try {
                log.info("Animando plano {} (intento {}/{}): seed={}", shot.getId(), a, maxAttempts, usedSeed);
                videoResponse = aiGatewayClient.generateVideoI2V(req);
                break;
            } catch (Exception exc) {
                log.warn("Fallo en intento {} de animación para plano {}: {}", a, shot.getId(), exc.getMessage());
                if (a == maxAttempts) {
                    log.error("Agotados los {} intentos para animar plano {}. Marcando DEGRADED.", maxAttempts, shot.getId());
                }
            }
        }

        shot.setSeed(usedSeed);
        shot.setAttempts(attemptUsed);

        if (videoResponse != null) {
            shot.setStatus(ShotStatus.ANIMATED);

            // 4. Guardar assets de CLIP y CLIP_LAST_FRAME
            saveOrUpdateAsset(episode, shot, AssetType.CLIP, videoResponse.clipPath(), Map.of(
                    "model", model,
                    "durationMs", videoResponse.durationMs(),
                    "frames", videoResponse.frames(),
                    "fps", fps,
                    "actionPrompt", actionPrompt,
                    "seed", usedSeed,
                    "attempts", attemptUsed
            ));

            saveOrUpdateAsset(episode, shot, AssetType.CLIP_LAST_FRAME, videoResponse.lastFramePath(), Map.of(
                    "sourceClip", videoResponse.clipPath(),
                    "frameIndex", videoResponse.frames() - 1
            ));

            Shot savedShot = shotRepository.save(shot);
            return new ShotAnimationResult(savedShot, videoResponse.clipPath(), videoResponse.lastFramePath());
        } else {
            shot.setStatus(ShotStatus.DEGRADED);
            Shot savedShot = shotRepository.save(shot);
            return new ShotAnimationResult(savedShot, null, null);
        }
    }

    public Shot regenerateShotAnimation(UUID shotId) {
        Shot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + shotId));

        Episode episode = shot.getScene().getEpisode();
        log.info("Regenerando animación del plano {} (Escena {}, Orden {})",
                shotId, shot.getScene().getOrderIndex(), shot.getOrderIndex());

        // Forzar nuevo seed
        shot.setSeed(null);

        // Si es CHAIN_LAST_FRAME, buscar el lastFrame del plano anterior en la misma escena
        String prevLastFrame = null;
        if (shot.getContinuityMode() == ContinuityMode.CHAIN_LAST_FRAME && shot.getOrderIndex() > 1) {
            var prevShotOpt = shotRepository.findBySceneIdOrderByOrderIndexAsc(shot.getScene().getId())
                    .stream()
                    .filter(s -> s.getOrderIndex() == shot.getOrderIndex() - 1)
                    .findFirst();
            if (prevShotOpt.isPresent()) {
                var lfAsset = assetRepository.findByShotIdAndType(prevShotOpt.get().getId(), AssetType.CLIP_LAST_FRAME);
                if (lfAsset.isPresent()) {
                    prevLastFrame = lfAsset.get().getPath();
                }
            }
        }

        try {
            return animateSingleShot(episode, shot, prevLastFrame).shot();
        } finally {
            try {
                aiGatewayClient.freeGpu();
            } catch (Exception ignored) {}
        }
    }

    @Transactional(readOnly = true)
    public ShotAnimationResponse getShotAnimation(UUID shotId) {
        Shot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + shotId));

        var clipAssetOpt = assetRepository.findByShotIdAndType(shotId, AssetType.CLIP);
        var lastFrameAssetOpt = assetRepository.findByShotIdAndType(shotId, AssetType.CLIP_LAST_FRAME);

        String clipPath = clipAssetOpt.map(Asset::getPath).orElse(null);
        String lastFramePath = lastFrameAssetOpt.map(Asset::getPath).orElse(null);

        int frames = 0;
        int durationMs = shot.getTargetDurationMs();
        String actionPrompt = shot.getActionPrompt();

        if (clipAssetOpt.isPresent() && clipAssetOpt.get().getMetaJson() != null) {
            try {
                Map<String, Object> meta = objectMapper.readValue(clipAssetOpt.get().getMetaJson(), new TypeReference<>() {});
                if (meta.containsKey("frames")) {
                    frames = ((Number) meta.get("frames")).intValue();
                }
                if (meta.containsKey("durationMs")) {
                    durationMs = ((Number) meta.get("durationMs")).intValue();
                }
                if (meta.containsKey("actionPrompt")) {
                    actionPrompt = (String) meta.get("actionPrompt");
                }
            } catch (Exception ignored) {}
        }

        return new ShotAnimationResponse(
                shot.getId(),
                shot.getScene().getId(),
                shot.getOrderIndex(),
                shot.getStatus(),
                shot.getContinuityMode(),
                clipPath,
                lastFramePath,
                frames,
                durationMs,
                shot.getSeed(),
                shot.getAttempts(),
                actionPrompt
        );
    }

    private void saveOrUpdateAsset(Episode episode, Shot shot, AssetType type, String path, Map<String, Object> metadata) {
        var existing = assetRepository.findByShotIdAndType(shot.getId(), type);
        Asset asset = existing.orElseGet(() -> new Asset(episode, shot, type, path));
        asset.setPath(path);

        try {
            asset.setMetaJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            asset.setMetaJson("{}");
        }
        assetRepository.save(asset);
    }
}
