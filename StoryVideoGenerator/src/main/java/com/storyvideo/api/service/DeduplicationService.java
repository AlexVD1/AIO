package com.storyvideo.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryFingerprint;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.dto.DeduplicationCheckResult;
import com.storyvideo.api.repository.StoryFingerprintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;

@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);

    private final StoryFingerprintRepository fingerprintRepository;
    private final ObjectMapper objectMapper;

    public DeduplicationService(
            StoryFingerprintRepository fingerprintRepository,
            ObjectMapper objectMapper) {
        this.fingerprintRepository = fingerprintRepository;
        this.objectMapper = objectMapper;
    }

    public DeduplicationCheckResult checkDeduplication(
            StoryGenre genre,
            String title,
            String premise,
            String twist,
            List<String> characterNames) {

        if (title == null || premise == null) {
            return DeduplicationCheckResult.ok();
        }

        String normTitle = normalizeText(title);
        String titleHash = sha256(normTitle);

        // 1. Detección exacta de título
        if (fingerprintRepository.existsByTitleHash(titleHash)) {
            return DeduplicationCheckResult.duplicate("El título de la historia ya existe exactamente en el archivo histórico", title);
        }

        String normPremise = normalizeText(premise);
        String premiseHash = sha256(normPremise);

        // 2. Detección exacta de premisa
        if (fingerprintRepository.existsByPremiseHash(premiseHash)) {
            return DeduplicationCheckResult.duplicate("La premisa narrativa coincide exactamente con una historia previa", title);
        }

        // 3. Verificación semántica y por similitud con historias recientes del mismo género
        List<StoryFingerprint> recentFingerprints = fingerprintRepository.findTop50ByGenreOrderByCreatedAtDesc(genre);
        double[] currentPremiseVector = computeVector(normPremise);
        double[] currentTwistVector = twist != null ? computeVector(normalizeText(twist)) : null;
        Set<String> currentCharacters = characterNames != null
                ? new HashSet<>(characterNames.stream().map(this::normalizeText).toList())
                : Set.of();

        for (StoryFingerprint fp : recentFingerprints) {
            // A. Similitud de Levenshtein en título (> 0.75 de coincidencia)
            double titleSimilarity = calculateLevenshteinSimilarity(normTitle, fp.getTitleNormalized());
            if (titleSimilarity > 0.75) {
                return DeduplicationCheckResult.duplicate(
                        String.format("Título excesivamente similar al título previo '%s' (similitud: %.2f)", fp.getTitleNormalized(), titleSimilarity),
                        fp.getTitleNormalized()
                );
            }

            // B. Similitud coseno en premisa (> 0.85)
            if (fp.getPremiseEmbedding() != null) {
                double[] fpPremiseVector = parseVector(fp.getPremiseEmbedding());
                if (fpPremiseVector != null) {
                    double premiseSim = cosineSimilarity(currentPremiseVector, fpPremiseVector);
                    if (premiseSim > 0.85) {
                        return DeduplicationCheckResult.duplicate(
                                String.format("Premisa semánticamente duplicada con historia previa '%s' (similitud: %.2f)", fp.getTitleNormalized(), premiseSim),
                                fp.getTitleNormalized()
                        );
                    }
                }
            }

            // C. Similitud en giro narrativo / twist (> 0.82)
            if (currentTwistVector != null && fp.getTwistEmbedding() != null) {
                double[] fpTwistVector = parseVector(fp.getTwistEmbedding());
                if (fpTwistVector != null) {
                    double twistSim = cosineSimilarity(currentTwistVector, fpTwistVector);
                    if (twistSim > 0.82) {
                        return DeduplicationCheckResult.duplicate(
                                String.format("El giro narrativo es prácticamente idéntico al de '%s' (similitud: %.2f)", fp.getTitleNormalized(), twistSim),
                                fp.getTitleNormalized()
                        );
                    }
                }
            }

            // D. Overlap excesivo de nombres de personajes (> 60% de intercesión)
            if (!currentCharacters.isEmpty() && fp.getCharacterNamesJson() != null) {
                Set<String> fpChars = parseCharacters(fp.getCharacterNamesJson());
                double jaccard = calculateJaccardSimilarity(currentCharacters, fpChars);
                if (jaccard > 0.60) {
                    return DeduplicationCheckResult.duplicate(
                            String.format("Conjunto de personajes excesivamente similar a '%s' (coincidencia: %.2f)", fp.getTitleNormalized(), jaccard),
                            fp.getTitleNormalized()
                    );
                }
            }
        }

        return DeduplicationCheckResult.ok();
    }

    public StoryFingerprint saveFingerprint(Story story, String twist, List<String> characterNames) {
        String normTitle = normalizeText(story.getTitle());
        String titleHash = sha256(normTitle);
        String normPremise = normalizeText(story.getPremise());
        String premiseHash = sha256(normPremise);

        double[] premiseVec = computeVector(normPremise);
        String premiseEmbeddingJson = serializeVector(premiseVec);

        String twistHash = null;
        String twistEmbeddingJson = null;
        if (twist != null && !twist.isBlank()) {
            String normTwist = normalizeText(twist);
            twistHash = sha256(normTwist);
            twistEmbeddingJson = serializeVector(computeVector(normTwist));
        }

        String characterNamesJson = null;
        if (characterNames != null && !characterNames.isEmpty()) {
            try {
                characterNamesJson = objectMapper.writeValueAsString(characterNames.stream().map(this::normalizeText).toList());
            } catch (JsonProcessingException e) {
                log.warn("Error al serializar personajes para fingerprint: {}", e.getMessage());
            }
        }

        StoryFingerprint fingerprint = new StoryFingerprint(
                story,
                normTitle,
                titleHash,
                premiseHash,
                premiseEmbeddingJson,
                twistHash,
                twistEmbeddingJson,
                characterNamesJson,
                story.getGenre()
        );

        return fingerprintRepository.save(fingerprint);
    }

    public String normalizeText(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text.toLowerCase().trim(), Normalizer.Form.NFD);
        // Eliminar diacríticos y acentos
        normalized = normalized.replaceAll("\\p{M}", "");
        // Conservar solo caracteres alfanuméricos y espacios
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        // Reducir espacios múltiples
        return normalized.replaceAll("\\s+", " ").trim();
    }

    public String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public double calculateLevenshteinSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;

        int distance = computeLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    public double cosineSimilarity(double[] v1, double[] v2) {
        if (v1 == null || v2 == null || v1.length == 0 || v1.length != v2.length) return 0.0;
        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }

    private double[] computeVector(String text) {
        // Generador de embedding por bolsa de n-gramas de caracteres (dimensión fija 128)
        int dim = 128;
        double[] vector = new double[dim];
        if (text == null || text.isBlank()) return vector;

        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            if (token.length() < 2) continue;
            for (int i = 0; i <= token.length() - 2; i++) {
                String bi = token.substring(i, i + 2);
                int bucket = Math.abs(bi.hashCode() % dim);
                vector[bucket] += 1.0;
            }
        }

        // Normalizar a vector unitario
        double norm = 0.0;
        for (double v : vector) norm += v * v;
        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (int i = 0; i < dim; i++) vector[i] /= norm;
        }

        return vector;
    }

    private String serializeVector(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private double[] parseVector(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> parseCharacters(String json) {
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            return Set.of();
        }
    }

    private int computeLevenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) costs[j] = j;
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
}
