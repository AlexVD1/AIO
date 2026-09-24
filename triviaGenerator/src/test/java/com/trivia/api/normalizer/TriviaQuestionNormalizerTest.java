package com.trivia.api.normalizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TriviaQuestionNormalizer — Tests unitarios de normalización")
class TriviaQuestionNormalizerTest {

    private TriviaQuestionNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TriviaQuestionNormalizer();
    }

    @Test
    @DisplayName("Elimina signos de interrogación iniciales y finales")
    void debeEliminarSignosDeInterrogacion() {
        String input = "¿Cuál es el planeta más cercano al Sol?";
        String expected = "cuál es el planeta más cercano al sol";

        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Convierte mayúsculas a minúsculas conservando acentos")
    void debeConvertirAMinusculasConservandoAcentos() {
        String input = "ÁRBOL Y OCÉANO EN JÚPITER";
        String expected = "árbol y océano en júpiter";

        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Conserva la letra 'ñ' y diéresis 'ü'")
    void debeConservarEñeYDiaresis() {
        String input = "¿El pingüino y el ñandú son aves?";
        String expected = "el pingüino y el ñandú son aves";

        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Elimina signos de puntuación como comas, puntos, exclamaciones y comillas")
    void debeEliminarPuntuacionDiversa() {
        String input = "¡Atención! ¿En qué año llegó el hombre a la Luna, según la NASA?";
        String expected = "atención en qué año llegó el hombre a la luna según la nasa";

        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Colapsa espacios múltiples, tabuladores y saltos de línea")
    void debeColapsarEspaciosMultiples() {
        String input = "   ¿Cuál    es\tel\n\ncontinente  más   grande?   ";
        String expected = "cuál es el continente más grande";

        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Preguntas idénticas con distinta puntuación o espaciado producen el mismo resultado")
    void debeProducirMismoResultadoParaVariacionesSuperficiales() {
        String var1 = "¿Cuál es el planeta más cercano al Sol?";
        String var2 = "Cuál es el planeta más cercano al Sol";
        String var3 = "  ¿cuál   es el planeta más cercano al sol?  ";
        String var4 = "¡Cuál es el planeta más cercano al sol!";

        String norm1 = normalizer.normalize(var1);
        String norm2 = normalizer.normalize(var2);
        String norm3 = normalizer.normalize(var3);
        String norm4 = normalizer.normalize(var4);

        assertThat(norm1).isEqualTo(norm2)
                         .isEqualTo(norm3)
                         .isEqualTo(norm4);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Lanza IllegalArgumentException si la pregunta es nula o en blanco")
    void debeLanzarExcepcionConEntradasInvalidas(String input) {
        assertThatThrownBy(() -> normalizer.normalize(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser nula ni vacía");
    }
}
