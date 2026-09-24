package com.trivia.api.service;

import com.trivia.api.domain.TipoTrivia;
import com.trivia.api.repository.TipoTriviaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService — Tests unitarios con auto-creación y detección de similitud")
class CatalogServiceTest {

    @Mock
    private TipoTriviaRepository tipoTriviaRepository;

    private CatalogService catalogService;

    private TipoTrivia tipoHistoria;
    private TipoTrivia tipoAstronomia;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(tipoTriviaRepository);
        tipoHistoria = new TipoTrivia("HISTORIA", "Historia Universal", "Historia general");
        tipoAstronomia = new TipoTrivia("ASTRONOMIA", "Astronomía", "Cosmología");
    }

    @Test
    @DisplayName("Reutiliza categoría existente cuando la entrada es idéntica")
    void debeReutilizarCategoriaExistenteExacta() {
        when(tipoTriviaRepository.findByActivoTrue()).thenReturn(List.of(tipoHistoria, tipoAstronomia));

        TipoTrivia resultado = catalogService.validarYObtenerTipo("HISTORIA");

        assertThat(resultado.getCodigo()).isEqualTo("HISTORIA");
        verify(tipoTriviaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Reutiliza categoría existente cuando la entrada es muy similar (typo o plural)")
    void debeReutilizarCategoriaExistenteSimilar() {
        when(tipoTriviaRepository.findByActivoTrue()).thenReturn(List.of(tipoHistoria, tipoAstronomia));

        TipoTrivia res1 = catalogService.validarYObtenerTipo("Historias");
        assertThat(res1.getCodigo()).isEqualTo("HISTORIA");

        TipoTrivia res2 = catalogService.validarYObtenerTipo("Astronomía");
        assertThat(res2.getCodigo()).isEqualTo("ASTRONOMIA");

        verify(tipoTriviaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crea automáticamente una nueva categoría cuando no hay ninguna similar")
    void debeCrearNuevaCategoriaCuandoNoExisteSimilar() {
        when(tipoTriviaRepository.findByActivoTrue()).thenReturn(List.of(tipoHistoria, tipoAstronomia));
        when(tipoTriviaRepository.saveAndFlush(any(TipoTrivia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipoTrivia nueva = catalogService.validarYObtenerTipo("Cine y Series");

        assertThat(nueva).isNotNull();
        assertThat(nueva.getCodigo()).isEqualTo("CINE_Y_SERIES");
        assertThat(nueva.getNombre()).isEqualTo("Cine Y Series");
        assertThat(nueva.getActivo()).isTrue();

        ArgumentCaptor<TipoTrivia> captor = ArgumentCaptor.forClass(TipoTrivia.class);
        verify(tipoTriviaRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("CINE_Y_SERIES");
    }

    @Test
    @DisplayName("Lanza excepción si la entrada es nula o vacía")
    void debeLanzarExcepcionSiEntradaEsVacia() {
        assertThatThrownBy(() -> catalogService.validarYObtenerTipo(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> catalogService.validarYObtenerTipo("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
