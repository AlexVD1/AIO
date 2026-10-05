package com.kidsanim.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Location;
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
import com.kidsanim.api.dto.ShotKeyframeResponse;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.gateway.AiGatewayClient.KeyframeAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.KeyframeAiResponse;
import com.kidsanim.api.gateway.AiGatewayClient.ReferenceImageSpec;
import com.kidsanim.api.gateway.AiGatewayClient.SimilarityAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.SimilarityAiResponse;
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
import java.math.BigDecimal;
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
public class KeyframeStageService {

    private static final Logger log = LoggerFactory.getLogger(KeyframeStageService.class);

    private final EpisodeRepository episodeRepository;
    private final SceneRepository sceneRepository;
    private final ShotRepository shotRepository;
    private final AssetRepository assetRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final AiGatewayClient aiGatewayClient;
    private final KidsPipelineProperties pipelineProperties;
    private final ObjectMapper objectMapper;

    public KeyframeStageService(EpisodeRepository episodeRepository,
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

    public Episode processEpisodeKeyframes(UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        log.info("Iniciando etapa KEYFRAMES para el episodio '{}' ({})", episode.getTitle(), episode.getId());

        PipelineJob job = new PipelineJob(episode, PipelineStage.KEYFRAMES);
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
        String lastGeneratedKeyframePath = null;

        try {
            for (Shot shot : allShots) {
                // Idempotencia: si ya tiene un keyframe válido en disco, reutilizarlo
                if (shot.getStatus() == ShotStatus.KEYFRAME_READY || shot.getStatus() == ShotStatus.DEGRADED) {
                    var existingKeyframe = assetRepository.findByShotIdAndType(shot.getId(), AssetType.KEYFRAME);
                    if (existingKeyframe.isPresent() && new File(existingKeyframe.get().getPath()).exists()) {
                        log.info("Plano {} ya cuenta con keyframe válido ({}), saltando...",
                                shot.getId(), existingKeyframe.get().getPath());
                        lastGeneratedKeyframePath = existingKeyframe.get().getPath();
                        completedShots++;
                        continue;
                    }
                }

                Shot processedShot = generateShotKeyframe(episode, shot, lastGeneratedKeyframePath);
                var keyframeAsset = assetRepository.findByShotIdAndType(processedShot.getId(), AssetType.KEYFRAME);
                if (keyframeAsset.isPresent()) {
                    lastGeneratedKeyframePath = keyframeAsset.get().getPath();
                }

                completedShots++;
                int progress = (int) Math.round(((double) completedShots / Math.max(1, totalShots)) * 100.0);
                job.setProgressPercent(progress);
                pipelineJobRepository.save(job);
            }
        } finally {
            // Liberar memoria VRAM en AI Gateway tras completar o si hay error
            try {
                aiGatewayClient.freeGpu();
            } catch (Exception ex) {
                log.warn("No se pudo liberar VRAM tras etapa KEYFRAMES: {}", ex.getMessage());
            }
        }

        job.setStatus(PipelineJobStatus.COMPLETED);
        job.setProgressPercent(100);
        job.setFinishedAt(OffsetDateTime.now());
        pipelineJobRepository.save(job);

        episode.setStatus(EpisodeStatus.ANIMATION);
        Episode savedEpisode = episodeRepository.save(episode);

        log.info("Etapa KEYFRAMES completada para episodio '{}'. Total planos procesados: {}. Nuevo estado: {}",
                savedEpisode.getTitle(), totalShots, savedEpisode.getStatus());

        return savedEpisode;
    }

    public Shot generateShotKeyframe(Episode episode, Shot shot, String lastGeneratedKeyframePath) {
        Scene scene = shot.getScene();
        int sceneOrder = scene != null ? scene.getOrderIndex() : 1;
        int shotOrder = shot.getOrderIndex();

        // 1. Manejo de continuidad encadenada CHAIN_LAST_FRAME
        if (shot.getContinuityMode() == ContinuityMode.CHAIN_LAST_FRAME) {
            log.info("Plano {} es CHAIN_LAST_FRAME. Saltando generación de keyframe independiente.", shot.getId());
            if (lastGeneratedKeyframePath != null) {
                saveOrUpdateAsset(episode, shot, AssetType.KEYFRAME, lastGeneratedKeyframePath, Map.of(
                        "continuityMode", ContinuityMode.CHAIN_LAST_FRAME.name(),
                        "source", "CHAIN"
                ));
            }
            shot.setStatus(ShotStatus.KEYFRAME_READY);
            shot.setAttempts(0);
            return shotRepository.save(shot);
        }

        // 2. Construcción de prompt positivo y negativo
        Character character = shot.getCharacter();
        Location location = scene != null ? scene.getLocation() : null;
        StyleProfile style = episode.getSeries() != null ? episode.getSeries().getStyleProfile() : null;

        List<String> promptParts = new ArrayList<>();
        if (character != null && character.getCanonicalPrompt() != null && !character.getCanonicalPrompt().isBlank()) {
            promptParts.add(character.getCanonicalPrompt().trim());
        }
        if (shot.getVisualPrompt() != null && !shot.getVisualPrompt().isBlank()) {
            promptParts.add(shot.getVisualPrompt().trim());
        }
        if (location != null && location.getPrompt() != null && !location.getPrompt().isBlank()) {
            promptParts.add(location.getPrompt().trim());
        }
        if (style != null && style.getStylePrompt() != null && !style.getStylePrompt().isBlank()) {
            promptParts.add(style.getStylePrompt().trim());
        }
        String prompt = String.join(", ", promptParts);

        String negativePrompt = (style != null && style.getNegativePrompt() != null && !style.getNegativePrompt().isBlank())
                ? style.getNegativePrompt()
                : "blurry, low quality, distorted, deformed, realistic, dark, scary, text, watermark";

        int width = style != null ? style.getWidth() : 1024;
        int height = style != null ? style.getHeight() : 576;

        List<ReferenceImageSpec> refImages = new ArrayList<>();
        if (character != null && character.getReferenceImagePath() != null && !character.getReferenceImagePath().isBlank()) {
            double weight = character.getIpadapterWeight() != null ? character.getIpadapterWeight().doubleValue() : 0.85;
            refImages.add(new ReferenceImageSpec(character.getReferenceImagePath(), weight));
        }

        String episodeDirName = episode.getId() != null ? episode.getId().toString() : "ep_temp";
        Path keyframesDir = Paths.get(pipelineProperties.storagePath(), "episodes", episodeDirName, "keyframes");
        keyframesDir.toFile().mkdirs();

        // 3. Generación iterativa con QA y reintentos
        int maxRetries = pipelineProperties.maxKeyframeRetries();
        double minSimilarity = pipelineProperties.characterSimilarityMin();

        String bestPath = null;
        double bestScore = -1.0;
        Long bestSeed = null;
        int bestAttempt = 1;
        boolean passed = false;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            long seed = (shot.getSeed() != null && attempt == 1)
                    ? shot.getSeed()
                    : (character != null && character.getReferenceSeed() != null
                            ? character.getReferenceSeed() + (shot.getOrderIndex() * 100L) + attempt
                            : (long) (Math.random() * 1_000_000_000L));

            Path candidateOut = keyframesDir.resolve(String.format("shot_%d_%d_attempt_%d.png", sceneOrder, shotOrder, attempt));
            KeyframeAiRequest req = new KeyframeAiRequest(
                    prompt,
                    negativePrompt,
                    refImages,
                    seed,
                    width,
                    height,
                    candidateOut.toAbsolutePath().toString()
            );

            log.info("Generando keyframe para plano {} (intento {}/{}): seed={}", shot.getId(), attempt, maxRetries, seed);
            KeyframeAiResponse res = aiGatewayClient.generateKeyframe(req);
            String candidatePath = res.imagePath();
            Long usedSeed = res.seed();

            // Evaluar similitud con imagen canónica si existe
            if (character != null && character.getReferenceImagePath() != null && new File(character.getReferenceImagePath()).exists()) {
                double score;
                try {
                    SimilarityAiResponse simRes = aiGatewayClient.calculateSimilarity(
                            new SimilarityAiRequest(candidatePath, character.getReferenceImagePath())
                    );
                    score = simRes.score() != null ? simRes.score() : 0.0;
                } catch (Exception qaEx) {
                    log.warn("QA de similitud no disponible para plano {} (intento {}): {}. Se cuenta como NO aprobado.",
                            shot.getId(), attempt, qaEx.getMessage());
                    score = 0.0;
                }
                log.info("Plano {} intento {}: similitud CLIP = {} (mínimo = {})", shot.getId(), attempt, score, minSimilarity);

                if (score >= bestScore) {
                    bestScore = score;
                    bestPath = candidatePath;
                    bestSeed = usedSeed;
                    bestAttempt = attempt;
                }

                if (score >= minSimilarity) {
                    passed = true;
                    log.info("Plano {} superó el umbral de similitud en el intento {}!", shot.getId(), attempt);
                    break;
                }
            } else {
                // Sin referencia de personaje, se acepta el primer intento
                bestScore = 1.0;
                bestPath = candidatePath;
                bestSeed = usedSeed;
                bestAttempt = attempt;
                passed = true;
                break;
            }
        }

        int totalAttemptsUsed = passed ? bestAttempt : maxRetries;
        shot.setSeed(bestSeed);
        shot.setAttempts(totalAttemptsUsed);
        if (bestScore >= 0.0) {
            shot.setSimilarityScore(BigDecimal.valueOf(bestScore));
        }

        if (passed) {
            shot.setStatus(ShotStatus.KEYFRAME_READY);
        } else {
            log.warn("Plano {} no alcanzó similitud mínima tras {} intentos (mejor score={}). Marcando DEGRADED.",
                    shot.getId(), maxRetries, bestScore);
            shot.setStatus(ShotStatus.DEGRADED);
        }

        Shot savedShot = shotRepository.save(shot);

        saveOrUpdateAsset(episode, savedShot, AssetType.KEYFRAME, bestPath, Map.of(
                "prompt", prompt,
                "negativePrompt", negativePrompt,
                "seed", bestSeed != null ? bestSeed : 0L,
                "similarityScore", bestScore >= 0.0 ? bestScore : 1.0,
                "attempts", totalAttemptsUsed,
                "passedQA", passed
        ));

        return savedShot;
    }

    public Shot regenerateShotKeyframe(UUID shotId) {
        Shot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + shotId));

        Episode episode = shot.getScene().getEpisode();
        log.info("Regenerando keyframe del plano {} (Escena {}, Orden {})",
                shotId, shot.getScene().getOrderIndex(), shot.getOrderIndex());

        // Forzar nuevo seed
        shot.setSeed(null);

        try {
            return generateShotKeyframe(episode, shot, null);
        } finally {
            try {
                aiGatewayClient.freeGpu();
            } catch (Exception ignored) {}
        }
    }

    @Transactional(readOnly = true)
    public ShotKeyframeResponse getShotKeyframe(UUID shotId) {
        Shot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + shotId));

        var keyframeAssetOpt = assetRepository.findByShotIdAndType(shotId, AssetType.KEYFRAME);
        String keyframePath = keyframeAssetOpt.map(Asset::getPath).orElse(null);
        String prompt = null;

        if (keyframeAssetOpt.isPresent() && keyframeAssetOpt.get().getMetaJson() != null) {
            try {
                Map<String, Object> meta = objectMapper.readValue(keyframeAssetOpt.get().getMetaJson(), new TypeReference<>() {});
                if (meta.containsKey("prompt")) {
                    prompt = (String) meta.get("prompt");
                }
            } catch (Exception ignored) {}
        }

        return new ShotKeyframeResponse(
                shot.getId(),
                shot.getScene().getId(),
                shot.getOrderIndex(),
                shot.getStatus(),
                shot.getContinuityMode(),
                shot.getCharacter() != null ? shot.getCharacter().getName() : null,
                keyframePath,
                shot.getSimilarityScore() != null ? shot.getSimilarityScore().doubleValue() : null,
                shot.getSeed(),
                shot.getAttempts(),
                prompt
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
