package com.trivia.api.service;

import com.trivia.api.domain.TipoTrivia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategorySimilarityMatcher — Pruebas de detección de similitud de categorías")
class CategorySimilarityMatcherTest {

    private final TipoTrivia historia = new TipoTrivia("HISTORIA", "Historia Universal", "Historia general");
    private final TipoTrivia astronomia = new TipoTrivia("ASTRONOMIA", "Astronomía", "Cosmología y planetas");
    private final TipoTrivia cienciaNatural = new TipoTrivia("CIENCIA_NATURAL", "Ciencias Naturales", "Biología y física");
    private final TipoTrivia animales = new TipoTrivia("ANIMALES", "Animales", "Zoología");

    @Test
    @DisplayName("Detecta coincidencia exacta ignorando mayúsculas y minúsculas")
    void debeDetectarCoincidenciaExacta() {
        assertThat(CategorySimilarityMatcher.esSimilar("HISTORIA", historia)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("historia", historia)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("Historia", historia)).isTrue();
    }

    @Test
    @DisplayName("Detecta coincidencia eliminando acentos")
    void debeDetectarCoincidenciaSinAcentos() {
        assertThat(CategorySimilarityMatcher.esSimilar("Astronomía", astronomia)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("ASTRONOMÍA", astronomia)).isTrue();
    }

    @Test
    @DisplayName("Detecta plurales y singulares como similares")
    void debeDetectarPluralesYSingulares() {
        assertThat(CategorySimilarityMatcher.esSimilar("Historias", historia)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("historias", historia)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("Animal", animales)).isTrue();
    }

    @Test
    @DisplayName("Detecta pequeños errores tipográficos (typos Levenshtein <= 2)")
    void debeDetectarTyposLevenshtein() {
        assertThat(CategorySimilarityMatcher.esSimilar("Hstoria", historia)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("Histori", historia)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("Astronomya", astronomia)).isTrue();
    }

    @Test
    @DisplayName("Detecta variaciones compuestas de nombres")
    void debeDetectarNombresCompuestos() {
        assertThat(CategorySimilarityMatcher.esSimilar("Ciencias Naturales", cienciaNatural)).isTrue();
        assertThat(CategorySimilarityMatcher.esSimilar("CIENCIA NATURAL", cienciaNatural)).isTrue();
    }

    @Test
    @DisplayName("No confunde categorías realmente nuevas y distintas")
    void noDebeConfundirCategoriasNuevas() {
        assertThat(CategorySimilarityMatcher.esSimilar("CINE", historia)).isFalse();
        assertThat(CategorySimilarityMatcher.esSimilar("CINE", astronomia)).isFalse();
        assertThat(CategorySimilarityMatcher.esSimilar("CINE", cienciaNatural)).isFalse();
        assertThat(CategorySimilarityMatcher.esSimilar("CINE", animales)).isFalse();

        assertThat(CategorySimilarityMatcher.esSimilar("FILOSOFIA", historia)).isFalse();
        assertThat(CategorySimilarityMatcher.esSimilar("VIDEOJUEGOS", historia)).isFalse();
        assertThat(CategorySimilarityMatcher.esSimilar("LITERATURA", historia)).isFalse();
        assertThat(CategorySimilarityMatcher.esSimilar("GASTRONOMIA", astronomia)).isFalse();
    }

    @Test
    @DisplayName("Formatea slugs y title cases correctamente")
    void debeFormatearSlugsYTitleCase() {
        assertThat(CategorySimilarityMatcher.toCanonicalSlug("Cine y Series"))
                .isEqualTo("CINE_Y_SERIES");
        assertThat(CategorySimilarityMatcher.toTitleCase("cine_y_series"))
                .isEqualTo("Cine Y Series");
        assertThat(CategorySimilarityMatcher.toTitleCase("FILOSOFIA"))
                .isEqualTo("Filosofia");
    }
}
