package com.kidsanim.api.service;

import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.CharacterStatus;
import com.kidsanim.api.dto.*;
import com.kidsanim.api.gateway.AiGatewayClient;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.CharacterRepository;
import com.kidsanim.api.repository.SeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CharacterServiceTest {

    private CharacterRepository characterRepository;
    private SeriesRepository seriesRepository;
    private AiGatewayClient aiGatewayClient;
    private CharacterService characterService;

    @BeforeEach
    void setUp() {
        characterRepository = mock(CharacterRepository.class);
        seriesRepository = mock(SeriesRepository.class);
        aiGatewayClient = mock(AiGatewayClient.class);
        characterService = new CharacterService(characterRepository, seriesRepository, aiGatewayClient);
    }

    @Test
    void createCharacterSavesInDraftStatus() {
        UUID seriesId = UUID.randomUUID();
        Series series = new Series();
        series.setId(seriesId);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(characterRepository.save(any(Character.class))).thenAnswer(inv -> {
            Character c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CreateCharacterRequest request = new CreateCharacterRequest("Tito", CharacterRole.HOST,
                "a cute red fox", "curious", "ef_dora", "-12%", "default",
                null, 42L, new BigDecimal("0.85"));

        CharacterResponse response = characterService.create(seriesId, request);

        assertThat(response.name()).isEqualTo("Tito");
        assertThat(response.status()).isEqualTo(CharacterStatus.DRAFT);
        assertThat(response.seriesId()).isEqualTo(seriesId);
    }

    @Test
    void generateSheetCallsAiGatewayAndReturnsCandidates() {
        UUID charId = UUID.randomUUID();
        Series series = new Series();
        StyleProfile style = new StyleProfile("3D Cartoon", "ckpt.safetensors", "3d style", "neg style");
        series.setStyleProfile(style);

        Character character = new Character(series, "Tito", CharacterRole.HOST, "cute fox");
        character.setId(charId);

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(aiGatewayClient.generateCharacterSheet(any())).thenReturn(
                new AiGatewayClient.SheetAiResponse(
                        List.of("/path/1.png", "/path/2.png", "/path/3.png", "/path/4.png"),
                        List.of(42L, 43L, 44L, 45L)
                )
        );

        GenerateSheetRequest request = new GenerateSheetRequest(42L, 4, 768, 768);
        CharacterSheetResponse response = characterService.generateSheet(charId, request);

        assertThat(response.characterId()).isEqualTo(charId);
        assertThat(response.characterName()).isEqualTo("Tito");
        assertThat(response.images()).hasSize(4);
        assertThat(response.seeds()).containsExactly(42L, 43L, 44L, 45L);
        verify(aiGatewayClient).generateCharacterSheet(any());
    }

    @Test
    void approveSetsImagePathSeedAndApprovedStatus() {
        UUID charId = UUID.randomUUID();
        Character character = new Character();
        character.setId(charId);
        character.setName("Tito");
        character.setStatus(CharacterStatus.DRAFT);

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(characterRepository.save(any(Character.class))).thenAnswer(inv -> inv.getArgument(0));

        ApproveCharacterRequest request = new ApproveCharacterRequest("/storage/tito_approved.png", 42L);
        CharacterResponse response = characterService.approve(charId, request);

        assertThat(response.status()).isEqualTo(CharacterStatus.APPROVED);
        assertThat(response.referenceImagePath()).isEqualTo("/storage/tito_approved.png");
        assertThat(response.referenceSeed()).isEqualTo(42L);
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID charId = UUID.randomUUID();
        when(characterRepository.findById(charId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.findById(charId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(charId.toString());
    }
}
