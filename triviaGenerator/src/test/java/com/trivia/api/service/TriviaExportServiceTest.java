package com.trivia.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trivia.api.domain.*;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriviaExportService — Tests unitarios de exportación en ZIP")
class TriviaExportServiceTest {

    @Mock
    private TriviaRepository triviaRepository;

    @Mock
    private TriviaOpcionRepository opcionRepository;

    @Mock
    private TriviaAssetRepository assetRepository;

    @Mock
    private AssetStorageService storageService;

    @Mock
    private TriviaRendererService rendererService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private TriviaExportService exportService;

    private Trivia trivia1;
    private UUID triviaId1;

    @BeforeEach
    void setUp() {
        triviaId1 = UUID.randomUUID();
        TipoTrivia tipo = new TipoTrivia("HISTORIA", "Historia", "Historia mundial");
        TriviaGeneration gen = new TriviaGeneration(tipo, 1, 4, Dificultad.MEDIA, "es-MX", null);
        trivia1 = new Trivia(gen, tipo, "¿Cuál es la capital del Imperio Inca?", "normalizada", "hash123", "Cuzco fue la capital.", Dificultad.MEDIA, "es-MX", "Incas");
        ReflectionTestUtils.setField(trivia1, "id", triviaId1);
    }

    @Test
    @DisplayName("exportarZip genera estructura plana con trivias.json, trivia_{id}.json y archivos trivia_{id}_question.png y answer.png")
    void exportarZipExitosoGeneraArchivosCorrectos() throws IOException {
        List<UUID> ids = List.of(triviaId1);
        when(triviaRepository.findAllById(ids)).thenReturn(List.of(trivia1));

        TriviaOpcion opcA = new TriviaOpcion(trivia1, "A", "Cuzco", true);
        TriviaOpcion opcB = new TriviaOpcion(trivia1, "B", "Lima", false);
        when(opcionRepository.findByTriviaOrderByLetraAsc(trivia1)).thenReturn(List.of(opcA, opcB));

        TriviaAsset assetQ = new TriviaAsset(trivia1, TipoAsset.PREGUNTA, "gen/q.png", "http://localhost/q.png");
        TriviaAsset assetA = new TriviaAsset(trivia1, TipoAsset.RESPUESTA, "gen/a.png", "http://localhost/a.png");
        when(assetRepository.findByTrivia(trivia1)).thenReturn(List.of(assetQ, assetA));

        byte[] fakePngQ = new byte[]{1, 2, 3, 4};
        byte[] fakePngA = new byte[]{5, 6, 7, 8};
        when(storageService.exists("gen/q.png")).thenReturn(true);
        when(storageService.retrieve("gen/q.png")).thenReturn(fakePngQ);
        when(storageService.exists("gen/a.png")).thenReturn(true);
        when(storageService.retrieve("gen/a.png")).thenReturn(fakePngA);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportService.exportarZip(ids, baos);

        byte[] zipBytes = baos.toByteArray();
        assertThat(zipBytes).isNotEmpty();

        // Inspeccionar el contenido del ZIP generado
        Set<String> entryNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                // Asegurar que no contiene subdirectorios para cada trivia
                assertThat(entry.getName()).doesNotContain("/");
                assertThat(entry.getName()).doesNotContain("\\");
            }
        }

        // Verificar archivos obligatorios
        assertThat(entryNames).contains("trivias.json");
        assertThat(entryNames).contains(String.format("%s.json", triviaId1));
        assertThat(entryNames).contains(String.format("%s_question.png", triviaId1));
        assertThat(entryNames).contains(String.format("%s_answer.png", triviaId1));
    }

    @Test
    @DisplayName("exportarZip lanza IllegalArgumentException si la lista de IDs es vacía")
    void exportarZipListaVaciaLanzaExcepcion() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertThatThrownBy(() -> exportService.exportarZip(List.of(), baos))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("exportarZip lanza NoSuchElementException si ninguna trivia existe")
    void exportarZipSinResultadosLanzaExcepcion() {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(triviaRepository.findAllById(ids)).thenReturn(List.of());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertThatThrownBy(() -> exportService.exportarZip(ids, baos))
                .isInstanceOf(NoSuchElementException.class);
    }
}
