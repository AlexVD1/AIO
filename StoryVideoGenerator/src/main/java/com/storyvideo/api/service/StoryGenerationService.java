package com.storyvideo.api.service;

import com.storyvideo.api.ai.StoryAiClient;
import com.storyvideo.api.ai.dto.AiCharacterItem;
import com.storyvideo.api.ai.dto.AiSceneItem;
import com.storyvideo.api.ai.dto.AiStoryGenerationRequest;
import com.storyvideo.api.ai.dto.AiStoryResponse;
import com.storyvideo.api.domain.*;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.StoryFingerprintRepository;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.repository.VisualStyleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class StoryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(StoryGenerationService.class);

    private final StoryAiClient aiClient;
    private final StoryValidationService validationService;
    private final StoryRepository storyRepository;
    private final VisualStyleRepository visualStyleRepository;
    private final DeduplicationService deduplicationService;
    private final AssetStorageService storageService;

    public StoryGenerationService(
            StoryAiClient aiClient,
            StoryValidationService validationService,
            StoryRepository storyRepository,
            VisualStyleRepository visualStyleRepository,
            DeduplicationService deduplicationService,
            AssetStorageService storageService) {
        this.aiClient = aiClient;
        this.validationService = validationService;
        this.storyRepository = storyRepository;
        this.visualStyleRepository = visualStyleRepository;
        this.deduplicationService = deduplicationService;
        this.storageService = storageService;
    }

    @Transactional
    public StoryResponseDto generateAndPersistStory(StoryGenerationRequestDto requestDto) {
        return generateAndPersistStory(requestDto, null);
    }

    @Transactional
    public StoryResponseDto generateAndPersistStory(StoryGenerationRequestDto requestDto, BatchJob batchJob) {
        log.info("Iniciando generación de historia para género: {}, tema: '{}'{}",
                requestDto.genre(), requestDto.theme(), batchJob != null ? " (Batch: " + batchJob.getId() + ")" : "");

        // 1. Resolver estilo visual
        VisualStyle visualStyle = resolveVisualStyle(requestDto.visualStyleName());

        // 2. Extraer memoria reciente de premisas para evitar duplicación
        List<String> recentPremises = storyRepository.findRecentPremisesByGenre(requestDto.genre(), 20);

        // 3. Preparar solicitud para el cliente de IA
        AiStoryGenerationRequest aiRequest = new AiStoryGenerationRequest(
                requestDto.genre(),
                requestDto.resolvedTone(),
                requestDto.theme(),
                requestDto.resolvedTargetDurationSeconds(),
                requestDto.resolvedLanguage(),
                requestDto.resolvedSceneCount(),
                recentPremises,
                visualStyle != null ? visualStyle.getQualityModifiers() : null
        );

        // 4. Invocar IA
        AiStoryResponse aiResponse = aiClient.generateStory(aiRequest);

        // 5. Validar estructura narrativa
        StoryValidationService.ValidationResult validation = validationService.validate(aiResponse);
        if (!validation.isValid()) {
            throw new IllegalStateException("La historia generada no superó los controles de calidad: " + String.join(", ", validation.errors()));
        }

        // 5.1 Validar deduplicación semántica e histórica
        List<String> characterNames = aiResponse.characters() != null
                ? aiResponse.characters().stream().map(AiCharacterItem::name).toList()
                : List.of();

        com.storyvideo.api.dto.DeduplicationCheckResult dedupResult = deduplicationService.checkDeduplication(
                requestDto.genre(),
                aiResponse.title(),
                aiResponse.premise(),
                aiResponse.twist(),
                characterNames
        );

        if (dedupResult.duplicate()) {
            log.warn("Historia generada rechazada por deduplicación: {}", dedupResult.reason());
            throw new IllegalStateException("Historia rechazada por duplicación: " + dedupResult.reason());
        }

        // 6. Construir y persistir Story
        Story story = new Story(
                aiResponse.title(),
                requestDto.genre(),
                requestDto.subgenre(),
                requestDto.resolvedTone(),
                requestDto.theme(),
                aiResponse.hook(),
                aiResponse.premise(),
                aiResponse.synopsis(),
                aiResponse.narrativeArc(),
                aiResponse.twist(),
                aiResponse.ending(),
                requestDto.resolvedLanguage(),
                requestDto.resolvedTargetDurationSeconds(),
                visualStyle
        );
        if (batchJob != null) {
            story.setBatchJob(batchJob);
        }
        story.setStatus(StoryStatus.PLANNING_SCENES);

        // 7. Asociar personajes
        if (aiResponse.characters() != null) {
            for (AiCharacterItem c : aiResponse.characters()) {
                story.addCharacter(new StoryCharacter(
                        c.name(),
                        c.physicalDescription(),
                        c.distinctiveFeatures(),
                        c.role(),
                        c.promptFragment()
                ));
            }
        }

        // 8. Asociar escenas
        int seq = 1;
        for (AiSceneItem s : aiResponse.scenes()) {
            StoryScene scene = new StoryScene(
                    s.sequenceNumber() > 0 ? s.sequenceNumber() : seq,
                    s.sceneType(),
                    s.narrationText(),
                    s.visualDescription(),
                    s.visualPrompt(),
                    s.cameraMovement()
            );
            scene.setNarrationEmotion(s.narrationEmotion());
            scene.setMusicIntensity(s.musicIntensity());
            scene.setAmbientSound(s.ambientSound());
            scene.setTransitionIn(s.transitionIn());
            scene.setTransitionOut(s.transitionOut());

            // Estimar duración provisional (aprox 12-15 palabras por cada 5 segundos de habla)
            double wordCount = s.narrationText() != null ? s.narrationText().split("\\s+").length : 0;
            double estSeconds = Math.max(4.0, (wordCount / 2.5) + 1.0);
            scene.setEstimatedDurationSeconds(BigDecimal.valueOf(estSeconds));

            story.addScene(scene);
            seq++;
        }

        Story savedStory = storyRepository.save(story);

        // 9. Registrar fingerprint con vectores semánticos
        try {
            deduplicationService.saveFingerprint(savedStory, aiResponse.twist(), characterNames);
        } catch (Exception e) {
            log.warn("No se pudo registrar fingerprint para historia {}: {}", savedStory.getId(), e.getMessage());
        }

        log.info("Historia '{}' persistida exitosamente con ID: {} ({} escenas)", savedStory.getTitle(), savedStory.getId(), savedStory.getScenes().size());
        return StoryResponseDto.fromEntity(savedStory, storageService);
    }

    @Transactional(readOnly = true)
    public StoryResponseDto getStoryById(UUID id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Historia no encontrada con ID: " + id));
        return StoryResponseDto.fromEntity(story, storageService);
    }

    private VisualStyle resolveVisualStyle(String name) {
        if (name != null && !name.isBlank()) {
            return visualStyleRepository.findByName(name.trim())
                    .orElseGet(() -> visualStyleRepository.findByIsDefaultTrue().orElse(null));
        }
        return visualStyleRepository.findByIsDefaultTrue().orElse(null);
    }
}
