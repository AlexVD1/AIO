package com.trivia.api.service;

import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.TipoAsset;
import com.trivia.api.domain.TipoTrivia;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaAsset;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.repository.TriviaAssetRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriviaMaintenanceService — Tests unitarios de re-renderizado")
class TriviaMaintenanceServiceTest {

    @Mock
    private TriviaRepository triviaRepository;

    @Mock
    private TriviaOpcionRepository opcionRepository;

    @Mock
    private TriviaAssetRepository assetRepository;

    @Mock
    private TriviaRendererService rendererService;

    @Mock
    private AssetStorageService storageService;

    @InjectMocks
    private TriviaMaintenanceService maintenanceService;

    private Trivia triviaMock;
    private List<TriviaOpcion> opcionesMock;

    @BeforeEach
    void setUp() {
        TipoTrivia tipo = new TipoTrivia("CIENCIA", "Ciencia", "Temas científicos");
        triviaMock = new Trivia(
                null,
                tipo,
                "¿Cuál es el elemento más abundante en el universo?",
                "cual es el elemento mas abundante en el universo",
                "hash-hidrogeno",
                "El hidrógeno representa alrededor del 75% de la masa elemental del universo.",
                Dificultad.FACIL,
                "es-MX",
                "Elementos"
        );
        // Usar reflection o setter si es necesario para id si id es nulo, pero aqui getId() funciona
        opcionesMock = List.of(
                new TriviaOpcion(triviaMock, "A", "Hidrógeno", true),
                new TriviaOpcion(triviaMock, "B", "Helio", false)
        );
    }

    @Test
    @DisplayName("rerenderTrivias procesa todas las trivias existentes cuando triviaIds es null")
    void rerenderTodasLasTriviasExitosamente() {
        when(triviaRepository.findAll()).thenReturn(List.of(triviaMock));
        when(opcionRepository.findByTriviaOrderByLetraAsc(triviaMock)).thenReturn(opcionesMock);
        when(rendererService.renderPregunta(eq(triviaMock), eq(opcionesMock))).thenReturn(new byte[]{1, 2, 3});
        when(rendererService.renderRespuesta(eq(triviaMock), eq(opcionesMock))).thenReturn(new byte[]{4, 5, 6});

        TriviaAsset assetPregunta = new TriviaAsset(triviaMock, TipoAsset.PREGUNTA, "path/pregunta.png", "http://localhost/path/pregunta.png");
        TriviaAsset assetRespuesta = new TriviaAsset(triviaMock, TipoAsset.RESPUESTA, "path/respuesta.png", "http://localhost/path/respuesta.png");

        when(assetRepository.findByTriviaAndTipo(triviaMock, TipoAsset.PREGUNTA)).thenReturn(Optional.of(assetPregunta));
        when(assetRepository.findByTriviaAndTipo(triviaMock, TipoAsset.RESPUESTA)).thenReturn(Optional.of(assetRespuesta));

        TriviaMaintenanceService.RerenderResult result = maintenanceService.rerenderTrivias(null);

        assertThat(result.totalTrivias()).isEqualTo(1);
        assertThat(result.totalImagenesProcesadas()).isEqualTo(2);
        assertThat(result.errores()).isEqualTo(0);

        verify(storageService, times(2)).store(any(byte[].class), anyString(), eq("image/png"));
    }

    @Test
    @DisplayName("rerenderTrivias maneja errores por trivia individual sin detener el proceso")
    void rerenderManejaErroresIndividuales() {
        when(triviaRepository.findAll()).thenReturn(List.of(triviaMock));
        when(opcionRepository.findByTriviaOrderByLetraAsc(triviaMock)).thenThrow(new RuntimeException("Simulated error"));

        TriviaMaintenanceService.RerenderResult result = maintenanceService.rerenderTrivias(null);

        assertThat(result.totalTrivias()).isEqualTo(1);
        assertThat(result.totalImagenesProcesadas()).isEqualTo(0);
        assertThat(result.errores()).isEqualTo(1);
    }
}
