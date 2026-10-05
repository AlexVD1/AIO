package com.kidsanim.api.video.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.video.validation.dto.VideoValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class QualityValidationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void validateNonExistentFileReturnsInvalid() {
        QualityValidationService service = new QualityValidationService(new KidsVideoProperties(), new ObjectMapper());
        Path nonExistent = tempDir.resolve("missing.mp4");

        VideoValidationResult result = service.validate(nonExistent, 10.0, 1920, 1080);
        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).anyMatch(issue -> issue.contains("no existe"));
    }

    @Test
    void validateTinyFileReturnsInvalid() throws Exception {
        QualityValidationService service = new QualityValidationService(new KidsVideoProperties(), new ObjectMapper());
        Path tinyFile = tempDir.resolve("tiny.mp4");
        Files.write(tinyFile, new byte[]{0, 1, 2, 3});

        VideoValidationResult result = service.validate(tinyFile, 10.0, 1920, 1080);
        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).anyMatch(issue -> issue.contains("demasiado pequeño"));
    }
}
