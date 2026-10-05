package com.kidsanim.api.service;

import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.CharacterStatus;
import com.kidsanim.api.dto.*;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.CharacterRepository;
import com.kidsanim.api.repository.SeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

    private final CharacterRepository characterRepository;
    private final SeriesRepository seriesRepository;
    private final AiGatewayClient aiGatewayClient;

    public CharacterService(CharacterRepository characterRepository,
                            SeriesRepository seriesRepository,
                            AiGatewayClient aiGatewayClient) {
        this.characterRepository = characterRepository;
        this.seriesRepository = seriesRepository;
        this.aiGatewayClient = aiGatewayClient;
    }

    @Transactional(readOnly = true)
    public List<CharacterResponse> findBySeriesId(UUID seriesId) {
        if (!seriesRepository.existsById(seriesId)) {
            throw new ResourceNotFoundException("Serie no encontrada con ID: " + seriesId);
        }
        return characterRepository.findBySeriesId(seriesId).stream()
                .map(CharacterResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CharacterResponse findById(UUID id) {
        return CharacterResponse.fromEntity(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Character getEntityById(UUID id) {
        return characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personaje no encontrado con ID: " + id));
    }

    public CharacterResponse create(UUID seriesId, CreateCharacterRequest request) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie no encontrada con ID: " + seriesId));

        Character character = new Character();
        character.setSeries(series);
        character.setName(request.name());
        character.setRole(request.role());
        character.setCanonicalPrompt(request.canonicalPrompt());
        character.setPersonality(request.personality());
        character.setTtsVoice(request.ttsVoice());
        character.setTtsRate(request.ttsRate());
        character.setTtsPitch(request.ttsPitch());
        character.setReferenceImagePath(request.referenceImagePath());
        character.setReferenceSeed(request.referenceSeed());
        character.setIpadapterWeight(request.ipadapterWeight());
        character.setStatus(CharacterStatus.DRAFT);

        Character saved = characterRepository.save(character);
        return CharacterResponse.fromEntity(saved);
    }

    public CharacterResponse update(UUID id, UpdateCharacterRequest request) {
        Character character = getEntityById(id);
        character.setName(request.name());
        character.setRole(request.role());
        character.setCanonicalPrompt(request.canonicalPrompt());
        character.setPersonality(request.personality());
        character.setTtsVoice(request.ttsVoice());
        character.setTtsRate(request.ttsRate());
        character.setTtsPitch(request.ttsPitch());
        character.setReferenceImagePath(request.referenceImagePath());
        character.setReferenceSeed(request.referenceSeed());
        character.setIpadapterWeight(request.ipadapterWeight());
        character.setStatus(request.status());

        Character updated = characterRepository.save(character);
        return CharacterResponse.fromEntity(updated);
    }

    public void delete(UUID id) {
        Character character = getEntityById(id);
        characterRepository.delete(character);
    }

    public CharacterSheetResponse generateSheet(UUID id, GenerateSheetRequest request) {
        Character character = getEntityById(id);
        Series series = character.getSeries();
        StyleProfile styleProfile = series.getStyleProfile();

        long baseSeed = (request != null && request.seed() != null)
                ? request.seed()
                : (character.getReferenceSeed() != null ? character.getReferenceSeed() : System.currentTimeMillis() % 1000000);

        int count = (request != null && request.count() != null && request.count() > 0)
                ? request.count()
                : 4;

        int width = (request != null && request.width() != null && request.width() > 0)
                ? request.width()
                : 768;

        int height = (request != null && request.height() != null && request.height() > 0)
                ? request.height()
                : 768;

        AiGatewayClient.SheetAiRequest aiRequest = new AiGatewayClient.SheetAiRequest(
                character.getCanonicalPrompt(),
                styleProfile != null ? styleProfile.getStylePrompt() : "",
                styleProfile != null ? styleProfile.getNegativePrompt() : "",
                baseSeed,
                width,
                height,
                null,
                count
        );

        AiGatewayClient.SheetAiResponse aiResponse = aiGatewayClient.generateCharacterSheet(aiRequest);

        log.info("Hoja de personaje generada para '{}': {} imágenes", character.getName(), aiResponse.images().size());
        return new CharacterSheetResponse(
                character.getId(),
                character.getName(),
                aiResponse.images(),
                aiResponse.seeds()
        );
    }

    public CharacterResponse approve(UUID id, ApproveCharacterRequest request) {
        Character character = getEntityById(id);
        character.setReferenceImagePath(request.imagePath());
        character.setReferenceSeed(request.seed());
        character.setStatus(CharacterStatus.APPROVED);

        Character saved = characterRepository.save(character);
        log.info("Personaje '{}' aprobado con imagen='{}', seed={}",
                character.getName(), request.imagePath(), request.seed());
        return CharacterResponse.fromEntity(saved);
    }
}
