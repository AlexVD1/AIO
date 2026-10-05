package com.kidsanim.api.script;

import com.kidsanim.api.domain.enums.EducationalTopicType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EpisodeFingerprintServiceTest {

    private EpisodeFingerprintService fingerprintService;

    @BeforeEach
    void setUp() {
        fingerprintService = new EpisodeFingerprintService();
    }

    @Test
    void generatesConsistentFingerprintIgnoringAccentsAndPunctuation() {
        UUID seriesId = UUID.randomUUID();

        String fp1 = fingerprintService.computeFingerprint(
                seriesId,
                EducationalTopicType.COUNTING,
                "Contar del 1 al 5 con manzanas",
                "¡Contamos manzanas con Tito!"
        );

        String fp2 = fingerprintService.computeFingerprint(
                seriesId,
                EducationalTopicType.COUNTING,
                "contar del 1 al 5 con manzanas.",
                "contamos manzanas con tito"
        );

        assertThat(fp1).isNotNull();
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void generatesDifferentFingerprintsForDifferentTopicsOrTitles() {
        UUID seriesId = UUID.randomUUID();

        String fp1 = fingerprintService.computeFingerprint(
                seriesId,
                EducationalTopicType.COUNTING,
                "Contar del 1 al 5",
                "Contando con Tito"
        );

        String fp2 = fingerprintService.computeFingerprint(
                seriesId,
                EducationalTopicType.COLORS,
                "El color rojo y azul",
                "Colores mágicos"
        );

        assertThat(fp1).isNotEqualTo(fp2);
    }
}
