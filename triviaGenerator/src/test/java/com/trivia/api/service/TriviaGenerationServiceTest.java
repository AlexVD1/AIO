package com.trivia.api.service;

import com.trivia.api.ai.TriviaAiClient;
import com.trivia.api.ai.dto.AiTriviaItem;
import com.trivia.api.ai.dto.AiTriviaOpcion;
import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.EstadoGeneracion;
import com.trivia.api.domain.TipoTrivia;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaGeneration;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.dto.TriviaGenerationRequest;
import com.trivia.api.dto.TriviaGenerationResponse;
import com.trivia.api.exception.InsufficientUniqueTriviasException;
import com.trivia.api.normalizer.TriviaQuestionNormalizer;
import com.trivia.api.repository.TriviaGenerationRepository;
import com.trivia.api.repository.TriviaOpcionRepository;
import com.trivia.api.repository.TriviaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriviaGenerationService — Tests unitarios del pipeline de generación")
class TriviaGenerationServiceTest {

    @Mock
    private CatalogService catalogService;

    @Mock
    private TriviaGenerationRepository generationRepository;

    @Mock
    private TriviaRepository triviaRepository;

    @Mock
    private TriviaOpcionRepository triviaOpcionRepository;

    @Mock
    private com.trivia.api.repository.TriviaAssetRepository triviaAssetRepository;

    @Mock
    private AssetStorageService assetStorageService;

    @Mock
    private TriviaAiClient aiClient;

    private TriviaValidationService validationService;
    private TriviaDuplicateService duplicateService;
    private TriviaRendererService rendererService;
    private com.trivia.api.ai.EmbeddingService embeddingService;
    private QuestionHashService hashService;
    private TriviaGenerationService generationService;

    private TipoTrivia tipoAstronomia;

    @BeforeEach
    void setUp() {
        TriviaQuestionNormalizer normalizer = new TriviaQuestionNormalizer();
        hashService = new QuestionHashService(normalizer);
        validationService = new TriviaValidationService();
        duplicateService = new TriviaDuplicateService(hashService, triviaRepository);
        rendererService = new TriviaRendererService();
        embeddingService = new com.trivia.api.ai.EmbeddingServiceImpl("", new com.fasterxml.jackson.databind.ObjectMapper());

        generationService = new TriviaGenerationService(
                catalogService,
                generationRepository,
                triviaRepository,
                triviaOpcionRepository,
                triviaAssetRepository,
                aiClient,
                validationService,
                duplicateService,
                rendererService,
                assetStorageService,
                embeddingService,
                50, // 50% buffer
                3   // 3 max attempts
        );

        tipoAstronomia = new TipoTrivia("ASTRONOMIA", "Astronomía", "Espacio y planetas");
        when(catalogService.validarYObtenerTipo("ASTRONOMIA")).thenReturn(tipoAstronomia);
        when(generationRepository.save(any(TriviaGeneration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(triviaRepository.save(any(Trivia.class)))
                .thenAnswer(invocation -> {
                    Trivia t = invocation.getArgument(0);
                    org.springframework.test.util.ReflectionTestUtils.setField(t, "id", java.util.UUID.randomUUID());
                    return t;
                });
        lenient().when(triviaOpcionRepository.save(any(TriviaOpcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(assetStorageService.store(any(), any(), any()))
                .thenReturn("http://localhost:8080/assets/mock.png");
    }

    private AiTriviaItem crearItem(String pregunta) {
        return new AiTriviaItem(
                pregunta,
                List.of(
                        new AiTriviaOpcion("A", "Opción A", true),
                        new AiTriviaOpcion("B", "Opción B", false)
                ),
                "Explicación educativa de " + pregunta
        );
    }

    @Test
    @DisplayName("Genera trivias exitosamente en el primer intento")
    void debeGenerarTriviasExitosamente() {
        TriviaGenerationRequest request = new TriviaGenerationRequest(
                "ASTRONOMIA", "Planetas", 2, 2, Dificultad.FACIL, "es-MX"
        );

        when(triviaRepository.findPreguntasContexto(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(triviaRepository.existsByPreguntaHash(anyString())).thenReturn(false);

        List<AiTriviaItem> respuestaLlm = List.of(
                crearItem("¿Cuál es el planeta más grande?"),
                crearItem("¿Cuál es el planeta más cercano al Sol?"),
                crearItem("¿Cuál es el planeta rojo?") // buffer adicional
        );
        when(aiClient.generate(any())).thenReturn(respuestaLlm);

        TriviaGenerationResponse response = generationService.generarTrivias(request);

        assertThat(response).isNotNull();
        assertThat(response.estado()).isEqualTo(EstadoGeneracion.COMPLETADA);
        assertThat(response.cantidadFinal()).isEqualTo(2);
        assertThat(response.intentos()).isEqualTo(1);
        assertThat(response.cantidadDescartada()).isZero();

        // Verifica que se guardaron 2 trivias y 4 opciones (2 por cada una)
        verify(triviaRepository, times(2)).save(any());
        verify(triviaOpcionRepository, times(4)).save(any());
    }

    @Test
    @DisplayName("Reintenta automáticamente si el primer intento tiene duplicados (retry loop con buffer)")
    void debeReintentarCuandoHayDuplicados() {
        TriviaGenerationRequest request = new TriviaGenerationRequest(
                "ASTRONOMIA", null, 2, 2, Dificultad.MEDIA, "es-MX"
        );

        when(triviaRepository.findPreguntasContexto(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        AiTriviaItem repetidaEnDb = crearItem("¿Pregunta repetida en BD?");
        AiTriviaItem nueva1 = crearItem("¿Primera pregunta única?");
        AiTriviaItem nueva2 = crearItem("¿Segunda pregunta única?");

        String hashRepetida = hashService.hashQuestion(repetidaEnDb.pregunta());
        String hashNueva1 = hashService.hashQuestion(nueva1.pregunta());
        String hashNueva2 = hashService.hashQuestion(nueva2.pregunta());

        // La repetida existe en BD; las nuevas no
        when(triviaRepository.existsByPreguntaHash(hashRepetida)).thenReturn(true);
        when(triviaRepository.existsByPreguntaHash(hashNueva1)).thenReturn(false);
        when(triviaRepository.existsByPreguntaHash(hashNueva2)).thenReturn(false);

        // Intento 1 devuelve la repetida y nueva1 -> solo 1 válida, falta 1
        // Intento 2 devuelve nueva2 -> se completa la cuota de 2
        when(aiClient.generate(any()))
                .thenReturn(List.of(repetidaEnDb, nueva1))
                .thenReturn(List.of(nueva2));

        TriviaGenerationResponse response = generationService.generarTrivias(request);

        assertThat(response.estado()).isEqualTo(EstadoGeneracion.COMPLETADA);
        assertThat(response.cantidadFinal()).isEqualTo(2);
        assertThat(response.intentos()).isEqualTo(2);
        assertThat(response.cantidadDescartada()).isEqualTo(1);

        verify(aiClient, times(2)).generate(any());
    }

    @Test
    @DisplayName("Lanza InsufficientUniqueTriviasException si supera el máximo de intentos")
    void debeFallarSiSuperaMaximoIntentos() {
        TriviaGenerationRequest request = new TriviaGenerationRequest(
                "ASTRONOMIA", null, 2, 2, Dificultad.DIFICIL, "es-MX"
        );

        when(triviaRepository.findPreguntasContexto(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        // El LLM siempre devuelve una pregunta que ya existe en la BD
        AiTriviaItem repetida = crearItem("¿Pregunta siempre duplicada?");
        when(triviaRepository.existsByPreguntaHash(anyString())).thenReturn(true);
        when(aiClient.generate(any())).thenReturn(List.of(repetida));

        assertThatThrownBy(() -> generationService.generarTrivias(request))
                .isInstanceOf(InsufficientUniqueTriviasException.class)
                .hasMessageContaining("INSUFFICIENT_UNIQUE_TRIVIAS");

        // Verificamos que se agotaron los 3 intentos máximos configurados
        verify(aiClient, times(3)).generate(any());
    }
}
