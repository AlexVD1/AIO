package com.kidsanim.api.script;

import com.kidsanim.api.domain.enums.EducationalTopicType;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EpisodeFingerprintService {

    public String computeFingerprint(UUID seriesId, EducationalTopicType topicType, String topicDetail, String title) {
        String normalizedDetail = normalize(topicDetail);
        String normalizedTitle = normalize(title);

        String raw = (seriesId != null ? seriesId.toString() : "global")
                + "|" + (topicType != null ? topicType.name() : "GENERAL")
                + "|" + normalizedDetail
                + "|" + normalizedTitle;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible en el sistema", e);
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
