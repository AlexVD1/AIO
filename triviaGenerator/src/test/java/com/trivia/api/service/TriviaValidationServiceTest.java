package com.trivia.api.service;

import com.trivia.api.ai.dto.AiTriviaItem;
import com.trivia.api.ai.dto.AiTriviaOpcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TriviaValidationService — Tests unitarios de validación")
class TriviaValidationServiceTest {

    private TriviaValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new TriviaValidationService();
    }

    private List<AiTriviaOpcion> crearOpcionesValidas() {
        return List.of(
                new AiTriviaOpcion("A", "Mercurio", true),
                new AiTriviaOpcion("B", "Venus", false),
                new AiTriviaOpcion("C", "Tierra", false),
                new AiTriviaOpcion("D", "Marte", false)
        );
    }

    @Test
    @DisplayName("Una trivia completa y correcta es válida")
    void triviaCompletaEsValida() {
        AiTriviaItem item = new AiTriviaItem(
                "¿Cuál es el planeta más cercano al Sol?",
                crearOpcionesValidas(),
                "Mercurio es el planeta más próximo al Sol."
        );

        assertThat(validationService.isValid(item, 4)).isTrue();
    }

    @Test
    @DisplayName("Descarta si el item es nulo o la pregunta está en blanco")
    void descartaPreguntaInvalida() {
        assertThat(validationService.isValid(null, 4)).isFalse();

        AiTriviaItem conPreguntaVacia = new AiTriviaItem(
                "   ",
                crearOpcionesValidas(),
                "Explicación válida."
        );
        assertThat(validationService.isValid(conPreguntaVacia, 4)).isFalse();
    }

    @Test
    @DisplayName("Descarta si la explicación es nula o vacía")
    void descartaExplicacionInvalida() {
        AiTriviaItem sinExplicacion = new AiTriviaItem(
                "¿Cuál es el planeta más grande?",
                crearOpcionesValidas(),
                ""
        );
        assertThat(validationService.isValid(sinExplicacion, 4)).isFalse();
    }

    @Test
    @DisplayName("Descarta si la cantidad de opciones no coincide con la esperada")
    void descartaCantidadOpcionesErronea() {
        AiTriviaItem item = new AiTriviaItem(
                "¿Pregunta con 4 opciones?",
                crearOpcionesValidas(),
                "Explicación válida."
        );

        assertThat(validationService.isValid(item, 3)).isFalse();
        assertThat(validationService.isValid(item, 5)).isFalse();
    }

    @Test
    @DisplayName("Descarta si las letras no son consecutivas (ej. A, B, D, E)")
    void descartaLetrasNoConsecutivas() {
        List<AiTriviaOpcion> opcionesMalas = List.of(
                new AiTriviaOpcion("A", "Op 1", true),
                new AiTriviaOpcion("B", "Op 2", false),
                new AiTriviaOpcion("D", "Op 3", false), // Salto a D
                new AiTriviaOpcion("E", "Op 4", false)
        );
        AiTriviaItem item = new AiTriviaItem("¿Pregunta?", opcionesMalas, "Explicación.");

        assertThat(validationService.isValid(item, 4)).isFalse();
    }

    @Test
    @DisplayName("Descarta si hay opciones con texto duplicado dentro de la misma pregunta")
    void descartaOpcionesConTextoDuplicado() {
        List<AiTriviaOpcion> opcionesDuplicadas = List.of(
                new AiTriviaOpcion("A", "París", true),
                new AiTriviaOpcion("B", "Londres", false),
                new AiTriviaOpcion("C", "PARÍS", false), // Duplicado de A
                new AiTriviaOpcion("D", "Berlín", false)
        );
        AiTriviaItem item = new AiTriviaItem("¿Capital de Francia?", opcionesDuplicadas, "Explicación.");

        assertThat(validationService.isValid(item, 4)).isFalse();
    }

    @Test
    @DisplayName("Descarta si no hay ninguna opción correcta o si hay más de una")
    void descartaSiNoHayExactamenteUnaCorrecta() {
        // Cero correctas
        List<AiTriviaOpcion> ceroCorrectas = List.of(
                new AiTriviaOpcion("A", "Op 1", false),
                new AiTriviaOpcion("B", "Op 2", false)
        );
        assertThat(validationService.isValid(new AiTriviaItem("¿P?", ceroCorrectas, "Exp."), 2)).isFalse();

        // Dos correctas
        List<AiTriviaOpcion> dosCorrectas = List.of(
                new AiTriviaOpcion("A", "Op 1", true),
                new AiTriviaOpcion("B", "Op 2", true)
        );
        assertThat(validationService.isValid(new AiTriviaItem("¿P?", dosCorrectas, "Exp."), 2)).isFalse();
    }

    @Test
    @DisplayName("filterValid retorna únicamente los items que cumplen todas las reglas")
    void filterValidFiltraCorrectamente() {
        AiTriviaItem valida = new AiTriviaItem("¿Pregunta válida?", crearOpcionesValidas(), "Explicación.");
        AiTriviaItem invalida = new AiTriviaItem("¿Pregunta sin exp?", crearOpcionesValidas(), "  ");

        List<AiTriviaItem> filtradas = validationService.filterValid(List.of(valida, invalida), 4);

        assertThat(filtradas).hasSize(1).containsExactly(valida);
    }
}
