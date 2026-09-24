package com.trivia.api.service;

import com.trivia.api.normalizer.TriviaQuestionNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestionHashService — Tests unitarios de hash SHA-256")
class QuestionHashServiceTest {

    private QuestionHashService hashService;

    @BeforeEach
    void setUp() {
        TriviaQuestionNormalizer normalizer = new TriviaQuestionNormalizer();
        hashService = new QuestionHashService(normalizer);
    }

    @Test
    @DisplayName("El hash calculado tiene una longitud exacta de 64 caracteres en hexadecimal")
    void debeGenerarHashDe64Caracteres() {
        String hash = hashService.calculateHash("cual es el planeta mas cercano al sol");

        assertThat(hash)
                .isNotNull()
                .hasSize(64)
                .matches("^[0-9a-f]{64}$");
    }

    @Test
    @DisplayName("El cálculo del hash es determinista (mismo input genera exactamente mismo hash)")
    void debeSerDeterminista() {
        String input = "cuál es el río más largo del mundo";

        String hash1 = hashService.calculateHash(input);
        String hash2 = hashService.calculateHash(input);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Inputs diferentes generan hashes completamente diferentes")
    void debeGenerarHashesDiferentesParaInputsDiferentes() {
        String hash1 = hashService.calculateHash("pregunta uno");
        String hash2 = hashService.calculateHash("pregunta dos");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("hashQuestion normaliza antes de hashear para unificar variaciones superficiales")
    void hashQuestionDebeUnificarVariaciones() {
        String q1 = "¿Cuál es la capital de Francia?";
        String q2 = "  cuál es la capital de Francia? ";
        String q3 = "¡Cual es la capital de Francia!"; // wait, q3 has "Cual" without accent if accents are kept

        // Las que tienen los mismos acentos pero distinta puntuación/mayúsculas/espacios:
        String hash1 = hashService.hashQuestion(q1);
        String hash2 = hashService.hashQuestion(q2);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("calculateHash conocido coincide con vector de prueba estándar")
    void debeCoincidirConVectorDePrueba() {
        // SHA-256 de "hello" = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        String hash = hashService.calculateHash("hello");
        assertThat(hash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si el texto a hashear es nulo")
    void debeLanzarExcepcionConNulo() {
        assertThatThrownBy(() -> hashService.calculateHash(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser nulo");
    }
}
