package com.kidsanim.api.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizador de texto para locución infantil (Fase Q2.5 - Texto para el oído).
 * Convierte dígitos a palabras en español para asegurar prosodia natural en TTS,
 * asegura signos dobles de interrogación/exclamación y limpia pausas.
 */
public final class SpanishSpokenTextNormalizer {

    private SpanishSpokenTextNormalizer() {
    }

    // Patrón para detectar dígitos aislados con su palabra siguiente
    private static final Pattern DIGIT_FOLLOWING_WORD = Pattern.compile("\\b(\\d+)\\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ]+)");
    private static final Pattern STANDALONE_DIGIT = Pattern.compile("\\b(\\d+)\\b");

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text.trim();

        // 1. Reemplazo de dígitos seguidos de sustantivo (manejo de concordancia de género para 1)
        Matcher matcher = DIGIT_FOLLOWING_WORD.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int num = parseNumber(matcher.group(1));
            String word = matcher.group(2);
            String numWord = numberToWord(num, word);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(numWord + " " + word));
        }
        matcher.appendTail(sb);
        result = sb.toString();

        // 2. Reemplazo de dígitos aislados restantes
        Matcher matcherDigits = STANDALONE_DIGIT.matcher(result);
        sb = new StringBuilder();
        while (matcherDigits.find()) {
            int num = parseNumber(matcherDigits.group(1));
            String numWord = numberToWord(num, null);
            matcherDigits.appendReplacement(sb, Matcher.quoteReplacement(numWord));
        }
        matcherDigits.appendTail(sb);
        result = sb.toString();

        // 3. Normalizar signos dobles españoles si empiezan con palabra interrogativa o exclamativa
        result = ensurePairedPunctuation(result);

        // 4. Limpieza de espacios múltiples y puntos suspensivos excesivos
        result = result.replaceAll("\\.{3,}", ", ");
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    private static int parseNumber(String digitStr) {
        try {
            return Integer.parseInt(digitStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String numberToWord(int num, String nextWord) {
        if (num < 0 || num > 20) {
            return String.valueOf(num);
        }

        boolean isFeminine = nextWord != null && (
                nextWord.toLowerCase().endsWith("a") ||
                nextWord.toLowerCase().endsWith("as") ||
                nextWord.equalsIgnoreCase("manzana") ||
                nextWord.equalsIgnoreCase("manzanas") ||
                nextWord.equalsIgnoreCase("flor") ||
                nextWord.equalsIgnoreCase("flores") ||
                nextWord.equalsIgnoreCase("estrella") ||
                nextWord.equalsIgnoreCase("estrellas")
        );

        return switch (num) {
            case 0 -> "cero";
            case 1 -> {
                if (nextWord == null) yield "uno";
                yield isFeminine ? "una" : "un";
            }
            case 2 -> "dos";
            case 3 -> "tres";
            case 4 -> "cuatro";
            case 5 -> "cinco";
            case 6 -> "seis";
            case 7 -> "siete";
            case 8 -> "ocho";
            case 9 -> "nueve";
            case 10 -> "diez";
            case 11 -> "once";
            case 12 -> "doce";
            case 13 -> "trece";
            case 14 -> "catorce";
            case 15 -> "quince";
            case 16 -> "dieciséis";
            case 17 -> "diecisiete";
            case 18 -> "dieciocho";
            case 19 -> "diecinueve";
            case 20 -> "veinte";
            default -> String.valueOf(num);
        };
    }

    private static String ensurePairedPunctuation(String text) {
        String t = text;
        // Si termina con '?' y no tiene '¿'
        if (t.endsWith("?") && !t.contains("¿")) {
            t = "¿" + t;
        }
        // Si termina con '!' y no tiene '¡'
        if (t.endsWith("!") && !t.contains("¡")) {
            t = "¡" + t;
        }
        return t;
    }
}
