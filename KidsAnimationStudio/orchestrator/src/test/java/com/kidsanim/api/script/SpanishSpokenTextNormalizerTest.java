package com.kidsanim.api.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpanishSpokenTextNormalizerTest {

    @Test
    void testConvertDigitsToWordsFeminine() {
        String input = "¡1 manzana roja y 2 manzanas verdes!";
        String output = SpanishSpokenTextNormalizer.normalize(input);
        assertEquals("¡una manzana roja y dos manzanas verdes!", output);
    }

    @Test
    void testConvertDigitsToWordsMasculine() {
        String input = "¡1 árbol grande y 3 pajaritos!";
        String output = SpanishSpokenTextNormalizer.normalize(input);
        assertEquals("¡un árbol grande y tres pajaritos!", output);
    }

    @Test
    void testCountingSequence() {
        String input = "¡1, 2, 3!";
        String output = SpanishSpokenTextNormalizer.normalize(input);
        assertEquals("¡uno, dos, tres!", output);
    }

    @Test
    void testEnsureSpanishQuestionMarks() {
        String input = "Cuántas manzanas ves en el árbol?";
        String output = SpanishSpokenTextNormalizer.normalize(input);
        assertEquals("¿Cuántas manzanas ves en el árbol?", output);
    }

    @Test
    void testExcessiveDotsBecomesComma() {
        String input = "Hola niños...... hoy contaremos juntos";
        String output = SpanishSpokenTextNormalizer.normalize(input);
        assertEquals("Hola niños, hoy contaremos juntos", output);
    }
}
