package com.trivia.api.service;

import com.trivia.api.domain.*;
import com.trivia.api.dto.StatsResponse;
import com.trivia.api.dto.TriviaResponse;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaGenerationRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriviaQueryService — Tests unitarios de consulta")
class TriviaQueryServiceTest {

    @Mock
    private TriviaRepository triviaRepository;

    @Mock
    private TriviaOpcionRepository triviaOpcionRepository;

    @Mock
    private TriviaAssetRepository triviaAssetRepository;

    @Mock
    private TriviaGenerationRepository generationRepository;

    private TriviaQueryService queryService;
    private TipoTrivia tipoGeografia;
    private Trivia triviaMock;

    @BeforeEach
    void setUp() {
        queryService = new TriviaQueryService(
                triviaRepository,
                triviaOpcionRepository,
                triviaAssetRepository,
                generationRepository
        );

        tipoGeografia = new TipoTrivia("GEOGRAFIA", "Geografía", "Países y capitales");
        triviaMock = new Trivia(
                null,
                tipoGeografia,
                "¿Cuál es el río más largo del mundo?",
                "cual es el rio mas largo del mundo",
                "hash123",
                "El río Amazonas es el más largo.",
                Dificultad.FACIL,
                "es-MX",
                "Ríos"
        );
    }

    @Test
    @DisplayName("obtenerPorId retorna el DTO completo con sus opciones y assets")
    void obtenerPorIdRetornaDetalle() {
        UUID id = UUID.randomUUID();
        when(triviaRepository.findById(id)).thenReturn(Optional.of(triviaMock));

        TriviaOpcion opcA = new TriviaOpcion(triviaMock, "A", "Amazonas", true);
        TriviaOpcion opcB = new TriviaOpcion(triviaMock, "B", "Nilo", false);
        when(triviaOpcionRepository.findByTriviaOrderByLetraAsc(triviaMock))
                .thenReturn(List.of(opcA, opcB));

        TriviaAsset assetPregunta = new TriviaAsset(triviaMock, TipoAsset.PREGUNTA, "path/q.png", "http://assets/q.png");
        when(triviaAssetRepository.findByTrivia(triviaMock))
                .thenReturn(List.of(assetPregunta));

        TriviaResponse response = queryService.obtenerPorId(id);

        assertThat(response).isNotNull();
        assertThat(response.pregunta()).isEqualTo("¿Cuál es el río más largo del mundo?");
        assertThat(response.opciones()).hasSize(2);
        assertThat(response.assets()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerPorId lanza NoSuchElementException si la trivia no existe")
    void obtenerPorIdInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(triviaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.obtenerPorId(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Trivia no encontrada con ID");
    }

    @Test
    @DisplayName("obtenerAleatoria retorna una trivia activa aleatoria")
    void obtenerAleatoriaRetornaTrivia() {
        when(triviaRepository.findRandom()).thenReturn(Optional.of(triviaMock));
        when(triviaOpcionRepository.findByTriviaOrderByLetraAsc(triviaMock)).thenReturn(List.of());
        when(triviaAssetRepository.findByTrivia(triviaMock)).thenReturn(List.of());

        TriviaResponse response = queryService.obtenerAleatoria();

        assertThat(response).isNotNull();
        assertThat(response.tipoTrivia()).isEqualTo("GEOGRAFIA");
    }

    @Test
    @DisplayName("obtenerAleatoria lanza NoSuchElementException si el repositorio está vacío")
    void obtenerAleatoriaVacioLanzaExcepcion() {
        when(triviaRepository.findRandom()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.obtenerAleatoria())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No existen trivias activas");
    }

    @Test
    @DisplayName("obtenerEstadisticas consolida correctamente métricas de la base de datos")
    void obtenerEstadisticasConsolidaMetricas() {
        when(triviaRepository.countByEstado(EstadoTrivia.ACTIVA)).thenReturn(100L);
        when(generationRepository.count()).thenReturn(25L);
        when(generationRepository.avgIntentosByEstadoCompletada()).thenReturn(1.3);
        when(generationRepository.avgCantidadDescartadaByEstadoCompletada()).thenReturn(0.8);
        when(triviaRepository.countByDificultadAndEstado(any(), eq(EstadoTrivia.ACTIVA))).thenReturn(33L);

        StatsResponse stats = queryService.obtenerEstadisticas();

        assertThat(stats.totalTriviasActivas()).isEqualTo(100L);
        assertThat(stats.totalGeneraciones()).isEqualTo(25L);
        assertThat(stats.promedioIntentosPorGeneracion()).isEqualTo(1.3);
        assertThat(stats.promedioDescartadasPorGeneracion()).isEqualTo(0.8);
        assertThat(stats.triviasPorDificultad()).containsKeys("FACIL", "MEDIA", "DIFICIL");
    }
}
