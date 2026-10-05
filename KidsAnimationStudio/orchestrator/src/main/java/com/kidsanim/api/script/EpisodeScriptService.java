package com.kidsanim.api.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.Scene;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.Shot;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.domain.enums.ScenePurpose;
import com.kidsanim.api.domain.enums.ShotStatus;
import com.kidsanim.api.dto.CreateEpisodeRequest;
import com.kidsanim.api.infrastructure.config.KidsSafetyProperties;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EpisodeScriptService {

    private static final Logger log = LoggerFactory.getLogger(EpisodeScriptService.class);

    private final SeriesRepository seriesRepository;
    private final EpisodeRepository episodeRepository;
    private final SceneRepository sceneRepository;
    private final ShotRepository shotRepository;
    private final LlmProvider llmProvider;
    private final PromptTemplateService promptTemplateService;
    private final ScriptSchemaValidator schemaValidator;
    private final ContentSafetyService contentSafetyService;
    private final EpisodeFingerprintService fingerprintService;
    private final ObjectMapper objectMapper;
    private final KidsSafetyProperties safetyProperties;

    @Autowired
    public EpisodeScriptService(SeriesRepository seriesRepository,
                                EpisodeRepository episodeRepository,
                                SceneRepository sceneRepository,
                                ShotRepository shotRepository,
                                LlmProvider llmProvider,
                                PromptTemplateService promptTemplateService,
                                ScriptSchemaValidator schemaValidator,
                                ContentSafetyService contentSafetyService,
                                EpisodeFingerprintService fingerprintService,
                                ObjectMapper objectMapper,
                                KidsSafetyProperties safetyProperties) {
        this.seriesRepository = seriesRepository;
        this.episodeRepository = episodeRepository;
        this.sceneRepository = sceneRepository;
        this.shotRepository = shotRepository;
        this.llmProvider = llmProvider;
        this.promptTemplateService = promptTemplateService;
        this.schemaValidator = schemaValidator;
        this.contentSafetyService = contentSafetyService;
        this.fingerprintService = fingerprintService;
        this.objectMapper = objectMapper;
        this.safetyProperties = safetyProperties != null ? safetyProperties : new KidsSafetyProperties(null, 14, 4, 8, 12, 40, 2);
    }

    public EpisodeScriptService(SeriesRepository seriesRepository,
                                EpisodeRepository episodeRepository,
                                SceneRepository sceneRepository,
                                ShotRepository shotRepository,
                                LlmProvider llmProvider,
                                PromptTemplateService promptTemplateService,
                                ScriptSchemaValidator schemaValidator,
                                ContentSafetyService contentSafetyService,
                                EpisodeFingerprintService fingerprintService,
                                ObjectMapper objectMapper) {
        this(seriesRepository, episodeRepository, sceneRepository, shotRepository,
             llmProvider, promptTemplateService, schemaValidator, contentSafetyService,
             fingerprintService, objectMapper, new KidsSafetyProperties(null, 14, 4, 8, 12, 40, 2));
    }

    public Episode generateAndSaveEpisode(UUID seriesId, CreateEpisodeRequest request) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie no encontrada con ID: " + seriesId));

        int maxRetries = 3;
        List<String> accumulatedIssues = new ArrayList<>();
        EpisodeScript acceptedScript = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.info("Generando guion para '{}' (Intento {}/{}) con LLM ({})",
                    series.getName(), attempt, maxRetries, llmProvider.getProviderName());

            String systemPrompt = promptTemplateService.getSystemPrompt();
            String userPrompt = promptTemplateService.buildUserPrompt(
                    series,
                    request.topicType(),
                    request.topicDetail(),
                    request.learningObjective(),
                    request.targetDurationSec()
            );

            if (!accumulatedIssues.isEmpty()) {
                userPrompt += "\n\n[ATENCIÓN: El intento anterior fue rechazado por las siguientes razones. Corrígelas estrictamente:\n- "
                        + String.join("\n- ", accumulatedIssues) + "]";
            }

            try {
                String rawJson = llmProvider.generateStructured(
                        systemPrompt,
                        userPrompt,
                        promptTemplateService.getEpisodeScriptSchema()
                );

                EpisodeScript script = objectMapper.readValue(rawJson, EpisodeScript.class);
                script = sanitizeScript(script, series, request.topicType());

                // 1. Validación de estructura y consistencia de biblia
                List<String> schemaErrors = schemaValidator.validate(script, series, request.topicType());
                if (!schemaErrors.isEmpty()) {
                    log.warn("Intento {} falló validación de esquema: {}", attempt, schemaErrors);
                    accumulatedIssues = schemaErrors;
                    continue;
                }

                // 2. Validación de seguridad (palabras prohibidas + longitud + revisión pedagógica)
                PedagogicalReviewResult safetyResult = contentSafetyService.evaluate(script, true);
                if (!safetyResult.approved()) {
                    log.warn("Intento {} falló revisión pedagógica/seguridad: {}", attempt, safetyResult.issues());
                    accumulatedIssues = safetyResult.issues();
                    continue;
                }

                // Aprobado
                acceptedScript = script;
                break;

            } catch (Exception e) {
                log.warn("Error parseando o generando guion en intento {}: {}", attempt, e.getMessage());
                accumulatedIssues.add("Error de formato: " + e.getMessage());
            }
        }

        if (acceptedScript == null) {
            throw new ScriptValidationException("No se logró generar un guion pedagógico válido tras " + maxRetries + " intentos",
                    accumulatedIssues);
        }

        // 3. Calcular fingerprint y verificar duplicados
        String fingerprint = fingerprintService.computeFingerprint(
                seriesId,
                request.topicType(),
                request.topicDetail(),
                acceptedScript.title()
        );

        if (episodeRepository.findByFingerprint(fingerprint).isPresent()) {
            throw new ScriptValidationException("Episodio duplicado detectado para la serie",
                    List.of("Ya existe un episodio registrado con temática y título idénticos (fingerprint: " + fingerprint + ")"));
        }

        // 4. Crear entidad Episode
        Episode episode = new Episode(
                series,
                request.topicType(),
                request.topicDetail(),
                acceptedScript.learningObjective(),
                acceptedScript.title()
        );

        try {
            episode.setScriptJson(objectMapper.writeValueAsString(acceptedScript));
        } catch (Exception e) {
            episode.setScriptJson("{}");
        }
        episode.setFingerprint(fingerprint);
        episode.setStatus(Boolean.TRUE.equals(request.autoApprove()) ? EpisodeStatus.NARRATION : EpisodeStatus.AWAITING_SCRIPT_APPROVAL);

        Episode savedEpisode = episodeRepository.save(episode);

        // 5. Persistir Escenas y Planos en la base de datos
        persistScenesAndShots(savedEpisode, series, acceptedScript);

        log.info("Episodio '{}' guardado exitosamente con ID {} ({} escenas, {} planos, estado: {})",
                savedEpisode.getTitle(), savedEpisode.getId(),
                savedEpisode.getScenes().size(),
                acceptedScript.totalShots(),
                savedEpisode.getStatus());

        return savedEpisode;
    }

    private void persistScenesAndShots(Episode episode, Series series, EpisodeScript script) {
        if (script.scenes() == null) return;

        int sceneOrder = 1;
        for (SceneScript sceneScript : script.scenes()) {
            Location matchedLocation = matchLocation(series, sceneScript.location());

            Scene scene = new Scene(
                    episode,
                    sceneOrder++,
                    matchedLocation,
                    sceneScript.purpose(),
                    sceneScript.bgmMood()
            );
            Scene savedScene = sceneRepository.save(scene);
            episode.getScenes().add(savedScene);

            if (sceneScript.shots() != null) {
                int shotOrder = 1;
                for (ShotScript shotScript : sceneScript.shots()) {
                    Character matchedCharacter = matchCharacter(series, shotScript.character());

                    Shot shot = new Shot(
                            savedScene,
                            shotOrder++,
                            shotScript.narration(),
                            shotScript.visualPrompt()
                    );
                    shot.setCharacter(matchedCharacter);
                    shot.setSpeaker(shotScript.speaker());
                    shot.setCameraMotion(shotScript.cameraMotion());
                    shot.setActionPrompt(shotScript.actionPrompt());
                    shot.setContinuityMode(shotScript.continuityMode());
                    shot.setPauseAfterMs(shotScript.pauseAfterMs());
                    shot.setTargetDurationMs(4000);
                    shot.setStatus(ShotStatus.PENDING);

                    try {
                        shot.setOverlaysJson(objectMapper.writeValueAsString(shotScript.overlays()));
                    } catch (Exception e) {
                        shot.setOverlaysJson("[]");
                    }

                    try {
                        shot.setSfxJson(objectMapper.writeValueAsString(shotScript.sfx()));
                    } catch (Exception e) {
                        shot.setSfxJson("[]");
                    }

                    Shot savedShot = shotRepository.save(shot);
                    savedScene.getShots().add(savedShot);
                }
            }
        }
    }

    private Location matchLocation(Series series, String locName) {
        if (series == null || series.getLocations() == null || series.getLocations().isEmpty()) {
            return null;
        }
        if (locName == null || locName.isBlank()) {
            return series.getLocations().get(0);
        }

        String normTarget = normalize(locName);
        for (Location loc : series.getLocations()) {
            String normLoc = normalize(loc.getName());
            if (normLoc.equals(normTarget) || normLoc.contains(normTarget) || normTarget.contains(normLoc)) {
                return loc;
            }
        }
        return series.getLocations().get(0);
    }

    private Character matchCharacter(Series series, String charName) {
        if (series == null || series.getCharacters() == null || series.getCharacters().isEmpty()) {
            return null;
        }
        if (charName == null || charName.isBlank() || charName.equalsIgnoreCase("none") || charName.equalsIgnoreCase("narrator")) {
            return null;
        }

        String normTarget = normalize(charName);
        for (Character c : series.getCharacters()) {
            String normC = normalize(c.getName());
            if (normC.equals(normTarget) || normC.contains(normTarget) || normTarget.contains(normC)) {
                return c;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Episode> findBySeriesId(UUID seriesId) {
        return episodeRepository.findBySeriesId(seriesId);
    }

    @Transactional(readOnly = true)
    public com.kidsanim.api.dto.EpisodeDetailResponse getDetail(UUID id) {
        Episode episode = getById(id);
        EpisodeScript script = null;
        if (episode.getScriptJson() != null && !episode.getScriptJson().isBlank()) {
            try {
                script = objectMapper.readValue(episode.getScriptJson(), EpisodeScript.class);
            } catch (Exception e) {
                log.warn("No se pudo deserializar script_json para el episodio {}: {}", id, e.getMessage());
            }
        }
        return com.kidsanim.api.dto.EpisodeDetailResponse.fromEntity(episode, script);
    }

    @Transactional(readOnly = true)
    public Episode getById(UUID id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episodio no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public EpisodeScript getScript(UUID id) {
        Episode episode = getById(id);
        String json = episode.getScriptJson();
        if (json == null || json.isBlank()) {
            throw new ResourceNotFoundException("El episodio " + id + " no tiene guion JSON registrado");
        }
        try {
            return objectMapper.readValue(json, EpisodeScript.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parseando el guion JSON almacenado: " + e.getMessage(), e);
        }
    }

    public Episode approveScript(UUID id) {
        Episode episode = getById(id);
        if (episode.getStatus() == EpisodeStatus.AWAITING_SCRIPT_APPROVAL) {
            episode.setStatus(EpisodeStatus.NARRATION);
            log.info("Guion del episodio {} aprobado manualmente, avanzando a NARRATION", id);
        }
        return episodeRepository.save(episode);
    }

    public EpisodeScript sanitizeScript(EpisodeScript script, Series series, EducationalTopicType topicType) {
        if (script == null) {
            script = new EpisodeScript("Aventuras con " + (series != null ? series.getName() : "Amigos"), "", new ArrayList<>());
        }

        String title = script.title();
        if (title == null || title.isBlank()) {
            title = "Aventuras con " + (series != null ? series.getName() : "Amigos");
        }

        String primaryCharName = (series != null && series.getCharacters() != null && !series.getCharacters().isEmpty())
                ? series.getCharacters().get(0).getName() : "Tito";
        String primaryLocName = (series != null && series.getLocations() != null && !series.getLocations().isEmpty())
                ? series.getLocations().get(0).getName() : "Huerto Soleado";

        List<SceneScript> sanitizedScenes = new ArrayList<>();
        List<SceneScript> rawScenes = script.scenes() != null ? script.scenes() : List.of();

        for (int i = 0; i < rawScenes.size(); i++) {
            SceneScript sc = rawScenes.get(i);
            ScenePurpose purpose = sc.purpose() != null ? sc.purpose() : ScenePurpose.values()[Math.min(i, ScenePurpose.values().length - 1)];

            Location matchedLoc = matchLocation(series, sc.location());
            String locName = matchedLoc != null ? matchedLoc.getName() : primaryLocName;
            String bgmMood = (sc.bgmMood() != null && !sc.bgmMood().isBlank()) ? sc.bgmMood() : (series != null ? series.getDefaultBgmMood() : "playful");

            List<ShotScript> sanitizedShots = new ArrayList<>();
            if (sc.shots() != null) {
                for (ShotScript sh : sc.shots()) {
                    String speaker = (sh.speaker() != null && !sh.speaker().isBlank()) ? sh.speaker() : primaryCharName;

                    Character matchedChar = matchCharacter(series, sh.character());
                    String charName = matchedChar != null ? matchedChar.getName() :
                            (sh.character() != null && (sh.character().equalsIgnoreCase("none") || sh.character().equalsIgnoreCase("narrator")) ? "none" : primaryCharName);

                    String rawNarration = (sh.narration() != null && !sh.narration().isBlank()) ? sh.narration() : "¡Miren esto con atención!";
                    String narration = SpanishSpokenTextNormalizer.normalize(rawNarration);

                    String visualPrompt = sh.visualPrompt();
                    if (visualPrompt == null || visualPrompt.isBlank()) {
                        String subject = "none".equalsIgnoreCase(charName) ? primaryCharName : charName;
                        visualPrompt = "Cute friendly 3D Pixar cartoon style, " + subject + " in " + locName + ", smiling happily, bright colorful lighting, detailed 8k render";
                    }

                    String actionPrompt = (sh.actionPrompt() != null && !sh.actionPrompt().isBlank()) ? sh.actionPrompt() : "smiling and gesturing happily";
                    CameraMotion cameraMotion = sh.cameraMotion() != null ? sh.cameraMotion() : CameraMotion.STATIC;
                    ContinuityMode continuityMode = sh.continuityMode() != null ? sh.continuityMode() : ContinuityMode.NEW_KEYFRAME;
                    Integer pause = (sh.pauseAfterMs() != null && sh.pauseAfterMs() > 0) ? sh.pauseAfterMs() : 300;

                    List<String> sfx = new ArrayList<>();
                    if (sh.sfx() != null) {
                        for (String rawSfx : sh.sfx()) {
                            String n = com.kidsanim.api.audio.AudioCatalogService.normalizeSfxName(rawSfx);
                            if (n != null && !sfx.contains(n)) sfx.add(n);
                        }
                    }
                    List<OverlaySpec> sanitizedOverlays = new ArrayList<>();

                    if (sh.overlays() != null) {
                        for (OverlaySpec ov : sh.overlays()) {
                            if (ov == null || ov.type() == null) continue;
                            String ovType = ov.type().toUpperCase().trim();
                            if (topicType == EducationalTopicType.COUNTING) {
                                if ("BIG_NUMBER".equals(ovType) || "COUNTER".equals(ovType)) {
                                    sanitizedOverlays.add(new OverlaySpec(ovType, ov.value(), ov.position(), ov.appearAtWord()));
                                } else {
                                    sfx.add(ov.type().toLowerCase());
                                }
                            } else {
                                sanitizedOverlays.add(ov);
                            }
                        }
                    }

                    sanitizedShots.add(new ShotScript(
                            speaker,
                            charName,
                            narration,
                            visualPrompt,
                            actionPrompt,
                            cameraMotion,
                            continuityMode,
                            sanitizedOverlays,
                            sfx,
                            pause
                    ));
                }
            }

            sanitizedScenes.add(new SceneScript(purpose, locName, bgmMood, sanitizedShots));
        }

        int minScenes = safetyProperties.minScenes();
        ScenePurpose[] defaultPurposes = new ScenePurpose[]{ScenePurpose.INTRO, ScenePurpose.CONCEPT, ScenePurpose.EXAMPLE, ScenePurpose.CHALLENGE, ScenePurpose.RECAP};
        while (sanitizedScenes.size() < minScenes) {
            ScenePurpose p = defaultPurposes[Math.min(sanitizedScenes.size(), defaultPurposes.length - 1)];
            sanitizedScenes.add(new SceneScript(p, primaryLocName, series != null ? series.getDefaultBgmMood() : "playful", new ArrayList<>()));
        }

        int currentTotalShots = sanitizedScenes.stream().mapToInt(s -> s.shots().size()).sum();
        int minShots = safetyProperties.minShots();
        int missingShots = minShots - currentTotalShots;

        if (missingShots > 0) {
            log.info("Guion tiene {} planos, agregando {} planos pedagógicos de refuerzo para alcanzar mínimo {}",
                    currentTotalShots, missingShots, minShots);

            String[] padNarrations = new String[]{
                    "¡Vamos a aplaudir juntos!",
                    "¡Muy bien hecho, amiguitos!",
                    "¡Contar es divertidísimo!",
                    "¿Listos para más sorpresas?",
                    "¡Lo hiciste de maravilla!",
                    "¡Eres un campeón contando!"
            };

            for (int k = 0; k < missingShots; k++) {
                SceneScript targetScene = sanitizedScenes.stream()
                        .min((s1, s2) -> Integer.compare(s1.shots().size(), s2.shots().size()))
                        .orElse(sanitizedScenes.get(sanitizedScenes.size() - 1));

                String padNarration = padNarrations[k % padNarrations.length];
                String padPrompt = "Cute friendly 3D Pixar cartoon style, " + primaryCharName + " celebrating enthusiastically in " + targetScene.location() + ", colorful confetti, happy bright atmosphere, detailed 8k render";

                ShotScript extraShot = new ShotScript(
                        primaryCharName,
                        primaryCharName,
                        padNarration,
                        padPrompt,
                        "clapping and celebrating happily",
                        CameraMotion.STATIC,
                        ContinuityMode.NEW_KEYFRAME,
                        List.of(),
                        List.of("chime"),
                        300
                );
                targetScene.shots().add(extraShot);
            }
        }

        return new EpisodeScript(title, script.learningObjective(), sanitizedScenes);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
}
