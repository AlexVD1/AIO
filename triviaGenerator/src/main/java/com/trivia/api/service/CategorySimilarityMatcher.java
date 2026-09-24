package com.trivia.api.service;

import com.trivia.api.domain.TipoTrivia;

import java.text.Normalizer;
import java.util.*;

/**
 * Utilidad para normalizar, comparar y detectar similitud difusa (fuzzy)
 * entre categorías de trivia y prevenir duplicados conceptuales u ortográficos.
 */
public final class CategorySimilarityMatcher {

    private CategorySimilarityMatcher() {
        // Clase de utilería estática
    }

    /**
     * Determina si una entrada de texto es idéntica o muy similar a una categoría existente.
     *
     * Reglas de similitud:
     * 1. Coincidencia exacta por código o nombre (ignorando mayúsculas/minúsculas).
     * 2. Coincidencia tras remover acentos y caracteres especiales (slug canónico).
     * 3. Coincidencia de raíces léxicas / singular-plural (ej: 'Historias' vs 'HISTORIA', 'Animal' vs 'ANIMALES').
     * 4. Coincidencia de tokens / palabras componentes (ej: 'Ciencias Naturales' vs 'CIENCIA_NATURAL').
     * 5. Distancia de Levenshtein reducida (<= 2 para palabras largas, <= 1 para cortas).
     */
    public static boolean esSimilar(String entrada, TipoTrivia categoriaExistente) {
        if (entrada == null || categoriaExistente == null) {
            return false;
        }

        String inputLimpio = entrada.trim();
        if (inputLimpio.isEmpty()) {
            return false;
        }

        String codExistente = categoriaExistente.getCodigo();
        String nomExistente = categoriaExistente.getNombre();

        // 1. Coincidencia directa insensible a mayúsculas
        if (inputLimpio.equalsIgnoreCase(codExistente) || inputLimpio.equalsIgnoreCase(nomExistente)) {
            return true;
        }

        // 2. Normalización de slugs sin acentos
        String slugInput = toCanonicalSlug(inputLimpio);
        String slugCodigo = toCanonicalSlug(codExistente);
        String slugNombre = toCanonicalSlug(nomExistente);

        if (slugInput.equals(slugCodigo) || slugInput.equals(slugNombre)) {
            return true;
        }

        // 3. Comparación por singular / plural (raíz)
        String stemInput = toStem(slugInput);
        String stemCodigo = toStem(slugCodigo);
        String stemNombre = toStem(slugNombre);

        if (stemInput.equals(stemCodigo) || stemInput.equals(stemNombre)) {
            return true;
        }

        // 4. Comparación por conjunto de tokens (palabras clave)
        if (tokensCoinciden(slugInput, slugCodigo) || tokensCoinciden(slugInput, slugNombre)) {
            return true;
        }

        // 5. Distancia de Levenshtein sobre slugs normalizados
        if (distanciaEsCercana(slugInput, slugCodigo) || distanciaEsCercana(slugInput, slugNombre)) {
            return true;
        }

        return false;
    }

    /**
     * Convierte cualquier texto en un slug alfanumérico en mayúsculas sin diacríticos.
     * Ejemplo: "Ciencias Naturales!" -> "CIENCIAS_NATURALES"
     * Ejemplo: "Astronomía" -> "ASTRONOMIA"
     */
    public static String toCanonicalSlug(String texto) {
        if (texto == null) return "";
        // Descomponer acentos
        String descompuesto = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        // Quitar marcas de acento
        String sinAcentos = descompuesto.replaceAll("\\p{M}", "");
        // Reemplazar todo lo no alfanumérico por guión bajo
        String slug = sinAcentos.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        // Quitar guiones bajos redundantes al inicio y final
        return slug.replaceAll("^_+|_+$", "");
    }

    /**
     * Remueve desinencias de plural habituales en español ('ES', 'S') para palabras de longitud suficiente.
     */
    public static String toStem(String slug) {
        if (slug == null || slug.length() <= 3) return slug;

        if (slug.endsWith("ES") && slug.length() > 5) {
            return slug.substring(0, slug.length() - 2);
        }
        if (slug.endsWith("S") && !slug.endsWith("IS") && slug.length() > 4) {
            return slug.substring(0, slug.length() - 1);
        }
        return slug;
    }

    /**
     * Verifica si dos cadenas compuestas comparten los mismos tokens semánticos (con o sin plural).
     */
    private static boolean tokensCoinciden(String slugA, String slugB) {
        if (slugA.isEmpty() || slugB.isEmpty()) return false;

        String[] tokensA = slugA.split("_");
        String[] tokensB = slugB.split("_");

        if (tokensA.length != tokensB.length) {
            return false;
        }

        Set<String> stemsA = new HashSet<>();
        for (String t : tokensA) stemsA.add(toStem(t));

        Set<String> stemsB = new HashSet<>();
        for (String t : tokensB) stemsB.add(toStem(t));

        return stemsA.equals(stemsB);
    }

    /**
     * Compara mediante distancia de Levenshtein ponderada por longitud.
     * Exige que la primera letra coincida para evitar falsos positivos con raíces distintas
     * (ej: 'GASTRONOMIA' vs 'ASTRONOMIA').
     */
    private static boolean distanciaEsCercana(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return false;

        // Si la primera letra es diferente, representan raíces léxicas distintas
        if (a.charAt(0) != b.charAt(0)) {
            return false;
        }

        int lenMax = Math.max(a.length(), b.length());
        int dist = calcularLevenshtein(a, b);

        if (lenMax >= 6 && dist <= 2) {
            return true;
        }
        if (lenMax >= 4 && dist <= 1) {
            return true;
        }

        // Ratio de similitud >= 85%
        double ratio = 1.0 - ((double) dist / lenMax);
        return ratio >= 0.85;
    }

    /**
     * Algoritmo de distancia de Levenshtein O(N*M) optimizado en memoria.
     */
    public static int calcularLevenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    /**
     * Da formato legible (Title Case) para nombres nuevos de categorías.
     * Ejemplo: "CINE_Y_SERIES" -> "Cine Y Series"
     * Ejemplo: "filosofia" -> "Filosofia"
     */
    public static String toTitleCase(String entrada) {
        if (entrada == null || entrada.isBlank()) return "General";

        String clean = entrada.replace('_', ' ').replaceAll("\\s+", " ").trim();
        String[] words = clean.split(" ");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) {
                sb.append(w.substring(1).toLowerCase(Locale.ROOT));
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
