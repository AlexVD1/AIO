package com.trivia.api.normalizer;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizador de preguntas de trivia para la detección de duplicados exactos.
 *
 * PROPÓSITO:
 *   Garantiza que variaciones superficiales en la formulación de una pregunta
 *   produzcan la misma cadena normalizada y, por ende, el mismo hash SHA-256.
 *
 * REGLAS DE NORMALIZACIÓN:
 *   1. Validación: no permite nulos ni cadenas vacías/en blanco.
 *   2. Unicode NFC: unifica representaciones compuestas y descompuestas de caracteres
 *      (ej. "á" precompuesta U+00E1 vs "a" + acento combinatorio U+0061 U+0301).
 *   3. Minúsculas: convierte todo el texto a minúsculas usando Locale.ROOT.
 *   4. Puntuación: elimina signos de interrogación (¿, ?), exclamación (¡, !),
 *      comillas (“”, «», "", ''), puntos, comas, dos puntos, punto y coma y paréntesis.
 *   5. Espacios: colapsa múltiples espacios, tabuladores o saltos de línea a un solo espacio.
 *   6. Acentos: SE CONSERVAN (á, é, í, ó, ú, ü, ñ) según la decisión de arquitectura
 *      (en español cambian significado semántico y ortográfico).
 */
@Component
public class TriviaQuestionNormalizer {

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[¿?¡!\"'“”«»()\\[\\]{}:;,.\\`´^~]");
    private static final Pattern MULTI_WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Normaliza el texto de una pregunta.
     *
     * @param pregunta Texto original de la pregunta
     * @return Texto normalizado
     * @throws IllegalArgumentException si la pregunta es nula o vacía
     */
    public String normalize(String pregunta) {
        if (pregunta == null || pregunta.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede ser nula ni vacía para ser normalizada.");
        }

        // 1. Normalización Unicode a forma canónica compuesta (NFC)
        String nfc = Normalizer.normalize(pregunta.trim(), Normalizer.Form.NFC);

        // 2. Convertir a minúsculas
        String lower = nfc.toLowerCase(Locale.ROOT);

        // 3. Remover signos de puntuación irrelevantes
        String withoutPunctuation = PUNCTUATION_PATTERN.matcher(lower).replaceAll("");

        // 4. Colapsar espacios múltiples y recortar bordes
        return MULTI_WHITESPACE_PATTERN.matcher(withoutPunctuation).replaceAll(" ").trim();
    }
}
