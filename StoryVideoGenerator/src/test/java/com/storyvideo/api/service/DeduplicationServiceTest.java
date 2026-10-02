package com.storyvideo.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryFingerprint;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.dto.DeduplicationCheckResult;
import com.storyvideo.api.repository.StoryFingerprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    private StoryFingerprintRepository fingerprintRepository;

    private ObjectMapper objectMapper;
    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        deduplicationService = new DeduplicationService(fingerprintRepository, objectMapper);
    }

    @Test
    @DisplayName("Normaliza texto eliminando acentos, caracteres especiales y mayúsculas")
    void shouldNormalizeTextCorrectly() {
        String raw = "¡¿El HOTEL Embrujado de la Montaña 1920?!!";
        String normalized = deduplicationService.normalizeText(raw);
        assertThat(normalized).isEqualTo("el hotel embrujado de la montana 1920");
    }

    @Test
    @DisplayName("Detecta duplicado exacto por hash de título")
    void shouldDetectDuplicateByExactTitleHash() {
        when(fingerprintRepository.existsByTitleHash(anyString())).thenReturn(true);

        DeduplicationCheckResult result = deduplicationService.checkDeduplication(
                StoryGenre.HORROR, "La Cripta Olvidada", "Premisa original", null, List.of()
        );

        assertThat(result.duplicate()).isTrue();
        assertThat(result.reason()).contains("título");
    }

    @Test
    @DisplayName("Detecta duplicado por similitud Levenshtein en títulos similares")
    void shouldDetectDuplicateByLevenshteinSimilarity() {
        when(fingerprintRepository.existsByTitleHash(anyString())).thenReturn(false);
        when(fingerprintRepository.existsByPremiseHash(anyString())).thenReturn(false);

        StoryFingerprint existingFp = new StoryFingerprint(
                null, "el misterio de la mansion sombria", "hash1", "hash2",
                null, null, null, null, StoryGenre.MYSTERY
        );

        when(fingerprintRepository.findTop50ByGenreOrderByCreatedAtDesc(StoryGenre.MYSTERY))
                .thenReturn(List.of(existingFp));

        DeduplicationCheckResult result = deduplicationService.checkDeduplication(
                StoryGenre.MYSTERY, "El Misterio de la Mansión Sombría II", "Premisa distinta", null, List.of()
        );

        assertThat(result.duplicate()).isTrue();
        assertThat(result.reason()).contains("Título excesivamente similar");
    }

    @Test
    @DisplayName("Aprueba historias suficientemente distintas sin colisiones")
    void shouldApproveUniqueStory() {
        when(fingerprintRepository.existsByTitleHash(anyString())).thenReturn(false);
        when(fingerprintRepository.existsByPremiseHash(anyString())).thenReturn(false);
        when(fingerprintRepository.findTop50ByGenreOrderByCreatedAtDesc(StoryGenre.SCI_FI))
                .thenReturn(List.of());

        DeduplicationCheckResult result = deduplicationService.checkDeduplication(
                StoryGenre.SCI_FI, "El Último Satélite de Europa", "Una sonda descubre vida en el hielo cósmico", null, List.of()
        );

        assertThat(result.duplicate()).isFalse();
        assertThat(result.reason()).isNull();
    }

    @Test
    @DisplayName("Guarda el fingerprint con hashes y vectores serializados")
    void shouldSaveFingerprint() {
        Story story = new Story("El Reloj Parado", StoryGenre.HORROR, "Tiempo");
        story.setPremise("A las 3:33 AM el tiempo se detiene en toda la ciudad.");

        when(fingerprintRepository.save(any(StoryFingerprint.class))).thenAnswer(inv -> inv.getArgument(0));

        StoryFingerprint saved = deduplicationService.saveFingerprint(story, "El relojero era la muerte", List.of("Javier", "Elena"));

        assertThat(saved).isNotNull();
        assertThat(saved.getTitleNormalized()).isEqualTo("el reloj parado");
        assertThat(saved.getTitleHash()).isNotBlank();
        assertThat(saved.getPremiseHash()).isNotBlank();
        assertThat(saved.getPremiseEmbedding()).isNotBlank().contains("[");
        assertThat(saved.getCharacterNamesJson()).contains("javier");

        verify(fingerprintRepository).save(any(StoryFingerprint.class));
    }
}
