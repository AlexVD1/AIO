package com.trivia.api.service;

import com.trivia.api.dto.DeduplicationResult;
import com.trivia.api.normalizer.TriviaQuestionNormalizer;
import com.trivia.api.repository.TriviaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriviaDuplicateService — Tests unitarios de deduplicación")
class TriviaDuplicateServiceTest {

    @Mock
    private TriviaRepository triviaRepository;

    private TriviaDuplicateService duplicateService;
    private QuestionHashService hashService;

    @BeforeEach
    void setUp() {
        TriviaQuestionNormalizer normalizer = new TriviaQuestionNormalizer();
        hashService = new QuestionHashService(normalizer);
        duplicateService = new TriviaDuplicateService(hashService, triviaRepository);
    }

    @Test
    @DisplayName("existePreguntaEnBaseDeDatos devuelve true si el hash existe en el repositorio")
    void debeDetectarPreguntaExistenteEnBaseDeDatos() {
        String pregunta = "¿Cuál es el planeta más cercano al Sol?";
        String hashEsperado = hashService.hashQuestion(pregunta);

        when(triviaRepository.existsByPreguntaHash(hashEsperado)).thenReturn(true);

        boolean existe = duplicateService.existePreguntaEnBaseDeDatos(pregunta);

        assertThat(existe).isTrue();
        verify(triviaRepository).existsByPreguntaHash(hashEsperado);
    }

    @Test
    @DisplayName("existePreguntaEnBaseDeDatos devuelve false si el hash no existe")
    void debeDevolverFalseParaPreguntaNueva() {
        when(triviaRepository.existsByPreguntaHash(anyString())).thenReturn(false);

        boolean existe = duplicateService.existePreguntaEnBaseDeDatos("¿Cuál es la montaña más alta?");

        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("deduplicar acepta preguntas completamente únicas y no presentes en base de datos")
    void debeAceptarPreguntasUnicas() {
        List<String> preguntas = List.of(
                "¿Cuál es el río más largo del mundo?",
                "¿Quién pintó la Mona Lisa?",
                "¿En qué año empezó la Segunda Guerra Mundial?"
        );

        when(triviaRepository.existsByPreguntaHash(anyString())).thenReturn(false);

        DeduplicationResult<String> resultado = duplicateService.deduplicar(preguntas, Function.identity());

        assertThat(resultado.validos()).hasSize(3);
        assertThat(resultado.descartadosPorBatch()).isEmpty();
        assertThat(resultado.descartadosPorBaseDeDatos()).isEmpty();
        assertThat(resultado.totalDescartados()).isZero();
    }

    @Test
    @DisplayName("deduplicar descarta preguntas repetidas dentro del mismo lote (batch duplicate)")
    void debeDescartarDuplicadosIntraLote() {
        List<String> preguntas = List.of(
                "¿Cuál es el planeta más cercano al Sol?",
                "¿Cuál es la capital de España?",
                "  cuál es el planeta más cercano al sol? ", // Mismo hash que la primera
                "¡CUÁL ES EL PLANETA MÁS CERCANO AL SOL!"   // Mismo hash que la primera
        );

        when(triviaRepository.existsByPreguntaHash(anyString())).thenReturn(false);

        DeduplicationResult<String> resultado = duplicateService.deduplicar(preguntas, Function.identity());

        assertThat(resultado.validos()).hasSize(2)
                .containsExactly(
                        "¿Cuál es el planeta más cercano al Sol?",
                        "¿Cuál es la capital de España?"
                );
        assertThat(resultado.descartadosPorBatch()).hasSize(2)
                .containsExactly(
                        "  cuál es el planeta más cercano al sol? ",
                        "¡CUÁL ES EL PLANETA MÁS CERCANO AL SOL!"
                );
        assertThat(resultado.descartadosPorBaseDeDatos()).isEmpty();
        assertThat(resultado.totalDescartados()).isEqualTo(2);
    }

    @Test
    @DisplayName("deduplicar descarta preguntas existentes en base de datos")
    void debeDescartarDuplicadosDeBaseDeDatos() {
        String preguntaEnDb = "¿En qué año se descubrió América?";
        String preguntaNueva = "¿Cuál es el elemento químico más abundante en el universo?";

        String hashEnDb = hashService.hashQuestion(preguntaEnDb);
        String hashNueva = hashService.hashQuestion(preguntaNueva);

        when(triviaRepository.existsByPreguntaHash(hashEnDb)).thenReturn(true);
        when(triviaRepository.existsByPreguntaHash(hashNueva)).thenReturn(false);

        List<String> preguntas = List.of(preguntaEnDb, preguntaNueva);

        DeduplicationResult<String> resultado = duplicateService.deduplicar(preguntas, Function.identity());

        assertThat(resultado.validos()).hasSize(1).containsExactly(preguntaNueva);
        assertThat(resultado.descartadosPorBaseDeDatos()).hasSize(1).containsExactly(preguntaEnDb);
        assertThat(resultado.descartadosPorBatch()).isEmpty();
    }

    @Test
    @DisplayName("deduplicar clasifica correctamente descartes mixtos (batch + base de datos)")
    void debeClasificarDescartesMixtos() {
        String pDb = "¿Pregunta que ya está en BD?";
        String p1 = "¿Pregunta única uno?";
        String p2 = "¿Pregunta única dos?";
        String p1Repetida = "  pregunta única uno ";

        String hashDb = hashService.hashQuestion(pDb);
        String hash1 = hashService.hashQuestion(p1);
        String hash2 = hashService.hashQuestion(p2);

        when(triviaRepository.existsByPreguntaHash(hashDb)).thenReturn(true);
        when(triviaRepository.existsByPreguntaHash(hash1)).thenReturn(false);
        when(triviaRepository.existsByPreguntaHash(hash2)).thenReturn(false);

        List<String> batch = List.of(pDb, p1, p2, p1Repetida);

        DeduplicationResult<String> resultado = duplicateService.deduplicar(batch, Function.identity());

        assertThat(resultado.validos()).containsExactly(p1, p2);
        assertThat(resultado.descartadosPorBaseDeDatos()).containsExactly(pDb);
        assertThat(resultado.descartadosPorBatch()).containsExactly(p1Repetida);
        assertThat(resultado.totalDescartados()).isEqualTo(2);
    }

    @Test
    @DisplayName("deduplicar respeta hashes de intentos previos de la misma sesión")
    void debeDescartarSegunHashesDeSesionPrevia() {
        String preguntaAceptadaPrevia = "¿Cuál es el mamífero más grande?";
        String hashPrevio = hashService.hashQuestion(preguntaAceptadaPrevia);

        String preguntaNueva = "¿Cuánto dura un día en Mercurio?";

        when(triviaRepository.existsByPreguntaHash(anyString())).thenReturn(false);

        List<String> nuevoIntento = List.of(preguntaAceptadaPrevia, preguntaNueva);

        DeduplicationResult<String> resultado = duplicateService.deduplicar(
                nuevoIntento,
                Function.identity(),
                Set.of(hashPrevio)
        );

        assertThat(resultado.validos()).containsExactly(preguntaNueva);
        assertThat(resultado.descartadosPorBatch()).containsExactly(preguntaAceptadaPrevia);
        assertThat(resultado.totalDescartados()).isEqualTo(1);
    }

    @Test
    @DisplayName("deduplicar con lista nula o vacía devuelve listas vacías de forma segura")
    void debeManejarEntradasVacias() {
        DeduplicationResult<String> resVacio = duplicateService.deduplicar(List.of(), Function.identity());
        assertThat(resVacio.validos()).isEmpty();
        assertThat(resVacio.totalDescartados()).isZero();

        DeduplicationResult<String> resNulo = duplicateService.deduplicar(null, Function.identity());
        assertThat(resNulo.validos()).isEmpty();
        assertThat(resNulo.totalDescartados()).isZero();
    }
}
