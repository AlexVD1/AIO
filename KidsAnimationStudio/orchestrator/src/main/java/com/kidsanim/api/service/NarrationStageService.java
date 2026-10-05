package com.kidsanim.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Asset;
import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.PipelineJob;
import com.kidsanim.api.domain.Scene;
import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.PipelineJobStatus;
import com.kidsanim.api.domain.enums.PipelineStage;
import com.kidsanim.api.domain.enums.ShotStatus;
import com.kidsanim.api.dto.ShotNarrationResponse;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.gateway.AiGatewayClient.AlignAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.AlignAiResponse;
import com.kidsanim.api.gateway.AiGatewayClient.TtsAiRequest;
import com.kidsanim.api.gateway.AiGatewayClient.TtsAiResponse;
import com.kidsanim.api.gateway.AiGatewayClient.WordTimestamp;
import com.kidsanim.api.infrastructure.config.KidsPipelineProperties;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.repository.EpisodeRepository;
import com.kidsanim.api.repository.PipelineJobRepository;
import com.kidsanim.api.repository.SceneRepository;
import com.kidsanim.api.repository.ShotRepository;
import com.kidsanim.api.audio.VoicePostProcessingService;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class NarrationStageService {

    private static final Logger log = LoggerFactory.getLogger(NarrationStageService.class);

    private final EpisodeRepository episodeRepository;
    private final SceneRepository sceneRepository;
    private final ShotRepository shotRepository;
    private final AssetRepository assetRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final AiGatewayClient aiGatewayClient;
    private final KidsPipelineProperties pipelineProperties;
    private final ObjectMapper objectMapper;
    private final VoicePostProcessingService voicePostProcessingService;

    @Autowired
    public NarrationStageService(EpisodeRepository episodeRepository,
                                 SceneRepository sceneRepository,
                                 ShotRepository shotRepository,
                                 AssetRepository assetRepository,
                                 PipelineJobRepository pipelineJobRepository,
                                 AiGatewayClient aiGatewayClient,
                                 KidsPipelineProperties pipelineProperties,
                                 ObjectMapper objectMapper,
                                 VoicePostProcessingService voicePostProcessingService) {
        this.episodeRepository = episodeRepository;
        this.sceneRepository = sceneRepository;
        this.shotRepository = shotRepository;
        this.assetRepository = assetRepository;
        this.pipelineJobRepository = pipelineJobRepository;
        this.aiGatewayClient = aiGatewayClient;
        this.pipelineProperties = pipelineProperties;
        this.objectMapper = objectMapper;
        this.voicePostProcessingService = voicePostProcessingService != null
                ? voicePostProcessingService
                : new VoicePostProcessingService(new KidsVideoProperties());
    }

    public NarrationStageService(EpisodeRepository episodeRepository,
                                 SceneRepository sceneRepository,
                                 ShotRepository shotRepository,
                                 AssetRepository assetRepository,
                                 PipelineJobRepository pipelineJobRepository,
                                 AiGatewayClient aiGatewayClient,
                                 KidsPipelineProperties pipelineProperties,
                                 ObjectMapper objectMapper) {
        this(episodeRepository, sceneRepository, shotRepository, assetRepository,
             pipelineJobRepository, aiGatewayClient, pipelineProperties, objectMapper,
             new VoicePostProcessingService(new KidsVideoProperties()));
    }

    public Episode processEpisodeNarration(UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        log.info("Iniciando etapa NARRATION para el episodio '{}' ({})", episode.getTitle(), episode.getId());

        PipelineJob job = new PipelineJob(episode, PipelineStage.NARRATION);
        job.setStatus(PipelineJobStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        job.setProgressPercent(0);
        job = pipelineJobRepository.save(job);

        // Reunir y ordenar todos los planos del episodio
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

        for (Shot shot : allShots) {
            // Idempotencia: si el plano ya tiene audio válido generado, saltar
            if (shot.getStatus() == ShotStatus.NARRATED) {
                var existingAudio = assetRepository.findByShotIdAndType(shot.getId(), AssetType.NARRATION_AUDIO);
                if (existingAudio.isPresent() && new File(existingAudio.get().getPath()).exists()) {
                    log.info("Plano {} ya narrado previamente con asset existente ({}), saltando...",
                            shot.getId(), existingAudio.get().getPath());
                    completedShots++;
                    continue;
                }
            }

            narrateSingleShot(episode, shot);
            completedShots++;

            int progress = (int) Math.round(((double) completedShots / Math.max(1, totalShots)) * 100.0);
            job.setProgressPercent(progress);
            pipelineJobRepository.save(job);
        }

        // Finalizar trabajo de pipeline y actualizar estado del episodio
        job.setStatus(PipelineJobStatus.COMPLETED);
        job.setProgressPercent(100);
        job.setFinishedAt(OffsetDateTime.now());
        pipelineJobRepository.save(job);

        episode.setStatus(EpisodeStatus.KEYFRAMES);
        Episode savedEpisode = episodeRepository.save(episode);

        log.info("Etapa NARRATION completada para episodio '{}'. Total planos narrados: {}. Nuevo estado: {}",
                savedEpisode.getTitle(), totalShots, savedEpisode.getStatus());

        return savedEpisode;
    }

    public Shot narrateSingleShot(Episode episode, Shot shot) {
        Scene scene = shot.getScene();
        int sceneOrder = scene != null ? scene.getOrderIndex() : 1;
        int shotOrder = shot.getOrderIndex();

        // 1. Determinar voz, rate y pitch según speaker o personaje asignado
        String voice = pipelineProperties.defaultNarratorVoice();
        String rate = pipelineProperties.defaultNarratorRate();
        String pitch = "default";

        String voiceEngine = "KOKORO";
        String voiceReferencePath = null;

        Character character = shot.getCharacter();
        if (character != null) {
            if (character.getTtsVoice() != null && !character.getTtsVoice().isBlank()) {
                voice = character.getTtsVoice();
            }
            if (character.getTtsRate() != null && !character.getTtsRate().isBlank()) {
                rate = character.getTtsRate();
            }
            if (character.getTtsPitch() != null && !character.getTtsPitch().isBlank()) {
                pitch = character.getTtsPitch();
            }
            if (character.getVoiceEngine() != null && !character.getVoiceEngine().isBlank()) {
                voiceEngine = character.getVoiceEngine();
            }
            if (character.getVoiceReferencePath() != null && !character.getVoiceReferencePath().isBlank()) {
                voiceReferencePath = character.getVoiceReferencePath();
            }
        }

        // 2. Definir ruta absoluta destino de audio
        String episodeDirName = episode.getId() != null ? episode.getId().toString() : "ep_temp";
        Path audioDir = Paths.get(pipelineProperties.storagePath(), "episodes", episodeDirName, "audio");
        audioDir.toFile().mkdirs();
        Path audioFilePath = audioDir.resolve(String.format("shot_%d_%d.wav", sceneOrder, shotOrder));
        String absoluteAudioPath = audioFilePath.toAbsolutePath().toString();

        // 3. Generar audio TTS vía AI Gateway (Kokoro o Clonación según voiceEngine)
        String rawText = shot.getNarrationText();
        if (rawText == null || rawText.isBlank()) {
            rawText = "...";
        }
        String text = com.kidsanim.api.script.SpanishSpokenTextNormalizer.normalize(rawText);

        TtsAiRequest ttsRequest = new TtsAiRequest(text, voice, rate, pitch, absoluteAudioPath, voiceEngine, voiceReferencePath);
        TtsAiResponse ttsResponse = aiGatewayClient.generateTts(ttsRequest);

        // Post-proceso acústico (HPF 80Hz, compresión suave y normalización -16 LUFS)
        voicePostProcessingService.processVoiceAudio(Path.of(ttsResponse.audioPath()), Path.of(ttsResponse.audioPath()));

        // 4. Alinear timestamps palabra por palabra con Faster-Whisper vía AI Gateway
        AlignAiRequest alignRequest = new AlignAiRequest(ttsResponse.audioPath(), text, "es");
        AlignAiResponse alignResponse = aiGatewayClient.alignAudio(alignRequest);

        // 5. Calcular target_duration_ms: audioDuration + pauseAfterMs (mínimo 3000 ms)
        int audioDuration = ttsResponse.durationMs();
        int pauseMs = shot.getPauseAfterMs();
        int targetDuration = Math.max(3000, audioDuration + pauseMs);
        shot.setTargetDurationMs(targetDuration);
        shot.setStatus(ShotStatus.NARRATED);

        Shot savedShot = shotRepository.save(shot);

        // 6. Registrar o actualizar assets de NARRATION_AUDIO y WORD_TIMESTAMPS
        saveOrUpdateAsset(episode, savedShot, AssetType.NARRATION_AUDIO, ttsResponse.audioPath(), Map.of(
                "durationMs", audioDuration,
                "targetDurationMs", targetDuration,
                "voice", voice,
                "sampleRate", 24000
        ));

        saveOrUpdateAsset(episode, savedShot, AssetType.WORD_TIMESTAMPS, ttsResponse.audioPath(), Map.of(
                "words", alignResponse.words()
        ));

        log.info("Plano {} ({}.{}) narrado: audio={} ms, target={} ms, voz='{}', palabras alinedas={}",
                savedShot.getId(), sceneOrder, shotOrder, audioDuration, targetDuration, voice, alignResponse.words().size());

        return savedShot;
    }

    public Shot regenerateShotNarration(UUID shotId) {
        Shot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + shotId));

        Episode episode = shot.getScene().getEpisode();
        log.info("Regenerando locación y timestamps del plano {} (Escena {}, Orden {})",
                shotId, shot.getScene().getOrderIndex(), shot.getOrderIndex());

        return narrateSingleShot(episode, shot);
    }

    @Transactional(readOnly = true)
    public ShotNarrationResponse getShotNarration(UUID shotId) {
        Shot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + shotId));

        var audioAssetOpt = assetRepository.findByShotIdAndType(shotId, AssetType.NARRATION_AUDIO);
        var timestampAssetOpt = assetRepository.findByShotIdAndType(shotId, AssetType.WORD_TIMESTAMPS);

        String audioPath = audioAssetOpt.map(Asset::getPath).orElse(null);
        int audioDuration = 0;
        String voice = shot.getCharacter() != null ? shot.getCharacter().getTtsVoice() : pipelineProperties.defaultNarratorVoice();

        if (audioAssetOpt.isPresent() && audioAssetOpt.get().getMetaJson() != null) {
            try {
                Map<String, Object> meta = objectMapper.readValue(audioAssetOpt.get().getMetaJson(), new TypeReference<>() {});
                if (meta.containsKey("durationMs")) {
                    audioDuration = ((Number) meta.get("durationMs")).intValue();
                }
            } catch (Exception ignored) {}
        }

        List<WordTimestamp> timestamps = new ArrayList<>();
        if (timestampAssetOpt.isPresent() && timestampAssetOpt.get().getMetaJson() != null) {
            try {
                Map<String, Object> meta = objectMapper.readValue(timestampAssetOpt.get().getMetaJson(), new TypeReference<>() {});
                if (meta.containsKey("words")) {
                    String wordsJson = objectMapper.writeValueAsString(meta.get("words"));
                    timestamps = objectMapper.readValue(wordsJson, new TypeReference<List<WordTimestamp>>() {});
                }
            } catch (Exception ignored) {}
        }

        return new ShotNarrationResponse(
                shot.getId(),
                shot.getScene().getId(),
                shot.getOrderIndex(),
                shot.getSpeaker(),
                voice,
                shot.getNarrationText(),
                audioDuration,
                shot.getTargetDurationMs(),
                audioPath,
                timestamps
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

    @Transactional(readOnly = true)
    public String generateReadingScript(UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("GUION DE LECTURA — GRABACIÓN DE VOZ HUMANA (MODO RECORDED)\n");
        sb.append("Episodio: ").append(episode.getTitle()).append("\n");
        sb.append("Indicaciones: Lee pausado, con tono cálido, respetando las pausas marcadas.\n");
        sb.append("================================================================================\n\n");

        List<Scene> scenes = sceneRepository.findByEpisodeIdOrderByOrderIndexAsc(episodeId);
        for (Scene sc : scenes) {
            sb.append(String.format("--- ESCENA %d (%s) — Locación: %s ---\n",
                    sc.getOrderIndex(), sc.getPurpose(), sc.getLocation() != null ? sc.getLocation().getName() : "General"));
            List<Shot> shots = shotRepository.findBySceneIdOrderByOrderIndexAsc(sc.getId());
            for (Shot sh : shots) {
                String speaker = sh.getSpeaker() != null ? sh.getSpeaker() : "Narrador";
                int pause = sh.getPauseAfterMs() > 0 ? sh.getPauseAfterMs() : 300;
                sb.append(String.format("[%d.%d] %s: \"%s\" [Pausa: %d ms]\n",
                        sc.getOrderIndex(), sh.getOrderIndex(), speaker, sh.getNarrationText(), pause));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public Episode processRecordedAudioUpload(UUID episodeId, java.io.InputStream audioStream, String originalFilename) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + episodeId));

        log.info("Procesando subida de audio grabado para episodio '{}' ({})", episode.getTitle(), episodeId);
        Path audioDir = Paths.get(pipelineProperties.storagePath(), "episodes", episodeId.toString(), "audio");
        audioDir.toFile().mkdirs();
        Path masterWav = audioDir.resolve("recorded_master.wav");

        try {
            java.nio.file.Files.copy(audioStream, masterWav, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            voicePostProcessingService.processVoiceAudio(masterWav, masterWav);
        } catch (Exception e) {
            log.error("Error guardando audio grabado: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo guardar el archivo de audio grabado: " + e.getMessage(), e);
        }

        List<Scene> scenes = sceneRepository.findByEpisodeIdOrderByOrderIndexAsc(episodeId);
        List<Shot> allShots = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();
        for (Scene scene : scenes) {
            List<Shot> shots = shotRepository.findBySceneIdOrderByOrderIndexAsc(scene.getId());
            for (Shot shot : shots) {
                allShots.add(shot);
                if (shot.getNarrationText() != null && !shot.getNarrationText().isBlank()) {
                    fullTextBuilder.append(shot.getNarrationText()).append(" ");
                }
            }
        }

        AlignAiRequest alignRequest = new AlignAiRequest(masterWav.toAbsolutePath().toString(), fullTextBuilder.toString().trim(), "es");
        AlignAiResponse alignResponse = aiGatewayClient.alignAudio(alignRequest);

        int wordsPerShot = Math.max(1, alignResponse.words().size() / Math.max(1, allShots.size()));
        int currentWordIdx = 0;

        for (int i = 0; i < allShots.size(); i++) {
            Shot shot = allShots.get(i);
            int nextWordIdx = (i == allShots.size() - 1) ? alignResponse.words().size() : Math.min(alignResponse.words().size(), currentWordIdx + wordsPerShot);
            List<WordTimestamp> shotWords = (currentWordIdx < nextWordIdx)
                    ? alignResponse.words().subList(currentWordIdx, nextWordIdx)
                    : List.of();

            int shotDurationMs = 3000;
            if (!shotWords.isEmpty()) {
                shotDurationMs = Math.max(3000, shotWords.get(shotWords.size() - 1).endMs() - shotWords.get(0).startMs() + 300);
            }
            shot.setTargetDurationMs(shotDurationMs);
            shot.setStatus(ShotStatus.NARRATED);
            Shot savedShot = shotRepository.save(shot);

            saveOrUpdateAsset(episode, savedShot, AssetType.NARRATION_AUDIO, masterWav.toAbsolutePath().toString(), Map.of(
                    "durationMs", shotDurationMs,
                    "targetDurationMs", shotDurationMs,
                    "mode", "RECORDED"
            ));
            saveOrUpdateAsset(episode, savedShot, AssetType.WORD_TIMESTAMPS, masterWav.toAbsolutePath().toString(), Map.of(
                    "words", shotWords
            ));
            currentWordIdx = nextWordIdx;
        }

        episode.setStatus(EpisodeStatus.KEYFRAMES);
        return episodeRepository.save(episode);
    }
}
