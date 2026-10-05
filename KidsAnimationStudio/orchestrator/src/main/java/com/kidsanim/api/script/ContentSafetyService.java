package com.kidsanim.api.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.infrastructure.config.KidsSafetyProperties;
import com.kidsanim.api.llm.LlmProvider;
import com.kidsanim.api.script.model.EpisodeScript;
import com.kidsanim.api.script.model.PedagogicalReviewResult;
import com.kidsanim.api.script.model.SceneScript;
import com.kidsanim.api.script.model.ShotScript;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ContentSafetyService {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyService.class);
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");

    private final KidsSafetyProperties safetyProperties;
    private final ResourceLoader resourceLoader;
    private final PromptTemplateService promptTemplateService;
    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;

    private final Set<String> blocklist = new HashSet<>();

    public ContentSafetyService(KidsSafetyProperties safetyProperties,
                                ResourceLoader resourceLoader,
                                PromptTemplateService promptTemplateService,
                                LlmProvider llmProvider,
                                ObjectMapper objectMapper) {
        this.safetyProperties = safetyProperties;
        this.resourceLoader = resourceLoader;
        this.promptTemplateService = promptTemplateService;
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadBlocklist();
    }

    public synchronized void loadBlocklist() {
        blocklist.clear();
        String path = safetyProperties.blocklistPath();
        try {
            Resource resource = resourceLoader.getResource(path);
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            blocklist.add(normalize(trimmed));
                        }
                    }
                }
                log.info("Cargadas {} palabras en la lista negra de seguridad pedagógica desde {}", blocklist.size(), path);
            } else {
                log.warn("El archivo de lista negra {} no existe, usando lista negra en memoria", path);
                addDefaultBlocklistWords();
            }
        } catch (Exception e) {
            log.error("Error al cargar lista negra {}: {}", path, e.getMessage(), e);
            addDefaultBlocklistWords();
        }
    }

    public List<String> checkBlocklist(EpisodeScript script) {
        List<String> issues = new ArrayList<>();
        if (script == null || script.scenes() == null) return issues;

        // Comprueba título
        checkForForbiddenWords(script.title(), "título", issues);

        // Comprueba cada plano
        for (int sIdx = 0; sIdx < script.scenes().size(); sIdx++) {
            SceneScript scene = script.scenes().get(sIdx);
            if (scene.shots() == null) continue;

            for (int shIdx = 0; shIdx < scene.shots().size(); shIdx++) {
                ShotScript shot = scene.shots().get(shIdx);
                String label = String.format("plano %d.%d", sIdx + 1, shIdx + 1);

                checkForForbiddenWords(shot.narration(), "narración en " + label, issues);
                checkForForbiddenWords(shot.visualPrompt(), "visualPrompt en " + label, issues);
                checkForForbiddenWords(shot.actionPrompt(), "actionPrompt en " + label, issues);
            }
        }

        return issues;
    }

    public List<String> checkNarrationLength(EpisodeScript script) {
        List<String> issues = new ArrayList<>();
        if (script == null || script.scenes() == null) return issues;

        int maxWords = safetyProperties.maxNarrationWords();

        for (int sIdx = 0; sIdx < script.scenes().size(); sIdx++) {
            SceneScript scene = script.scenes().get(sIdx);
            if (scene.shots() == null) continue;

            for (int shIdx = 0; shIdx < scene.shots().size(); shIdx++) {
                ShotScript shot = scene.shots().get(shIdx);
                if (shot.narration() != null) {
                    String[] words = shot.narration().trim().split("\\s+");
                    if (words.length > maxWords) {
                        issues.add(String.format("La narración en plano %d.%d excede el límite de %d palabras (%d palabras): \"%s\"",
                                sIdx + 1, shIdx + 1, maxWords, words.length, shot.narration()));
                    }
                }
            }
        }

        return issues;
    }

    public PedagogicalReviewResult reviewWithLlm(EpisodeScript script) {
        try {
            String scriptJson = objectMapper.writeValueAsString(script);
            String systemPrompt = promptTemplateService.getReviewerPrompt();
            String userPrompt = "Revisa el siguiente guion para niños de 2 a 6 años:\n\n" + scriptJson;

            String response = llmProvider.generateStructured(
                    systemPrompt,
                    userPrompt,
                    promptTemplateService.getPedagogicalReviewSchema()
            );

            return objectMapper.readValue(response, PedagogicalReviewResult.class);
        } catch (Exception e) {
            log.warn("Fallo en la revisión pedagógica del LLM, se aprueba con advertencia técnica: {}", e.getMessage());
            return PedagogicalReviewResult.ok();
        }
    }

    public PedagogicalReviewResult evaluate(EpisodeScript script, boolean executeLlmReview) {
        List<String> allIssues = new ArrayList<>();

        // 1. Longitud de narración
        allIssues.addAll(checkNarrationLength(script));

        // 2. Lista negra
        allIssues.addAll(checkBlocklist(script));

        if (!allIssues.isEmpty()) {
            return PedagogicalReviewResult.rejected(allIssues);
        }

        // 3. Revisión LLM opcional
        if (executeLlmReview && llmProvider.isAvailable()) {
            PedagogicalReviewResult llmResult = reviewWithLlm(script);
            if (!llmResult.approved()) {
                allIssues.addAll(llmResult.issues());
                return PedagogicalReviewResult.rejected(allIssues);
            }
        }

        return PedagogicalReviewResult.ok();
    }

    private void checkForForbiddenWords(String text, String locationDescription, List<String> issues) {
        if (text == null || text.isBlank()) return;

        String normalized = normalize(text);
        String[] tokens = PUNCTUATION_PATTERN.split(normalized);

        for (String token : tokens) {
            if (token.length() > 2 && blocklist.contains(token)) {
                issues.add(String.format("Palabra prohibida '%s' encontrada en %s", token, locationDescription));
            }
        }

        // También verifica frases compuestas de la lista negra (ej. "paw patrol", "baby shark")
        for (String blocked : blocklist) {
            if (blocked.contains(" ") && normalized.contains(blocked)) {
                issues.add(String.format("Término prohibido '%s' encontrado en %s", blocked, locationDescription));
            }
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").trim();
    }

    private void addDefaultBlocklistWords() {
        blocklist.addAll(List.of(
                "miedo", "terror", "monstruo", "bruja", "fantasma", "susto", "sangre",
                "arma", "pistola", "cuchillo", "muerte", "morir", "matar", "veneno",
                "peppa", "bluey", "paw patrol", "disney", "cocomelon", "spiderman"
        ));
    }
}
