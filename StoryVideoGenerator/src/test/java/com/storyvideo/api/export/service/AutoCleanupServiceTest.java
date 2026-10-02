package com.storyvideo.api.export.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AutoCleanupServiceTest {

    @TempDir
    Path tempExportDir;

    @TempDir
    Path tempStorageDir;

    private AutoCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new AutoCleanupService(
                tempExportDir.toString(),
                tempStorageDir.toString(),
                7,
                1024,
                true
        );
    }

    @Test
    @DisplayName("Elimina archivos exportados con más de 7 días y preserva los recientes")
    void shouldDeleteExportsOlderThanRetentionDays() throws IOException {
        Path oldVideo = tempExportDir.resolve("antiguo.mp4");
        Path oldMeta = tempExportDir.resolve("antiguo_metadata.json");
        Path recentVideo = tempExportDir.resolve("reciente.mp4");

        Files.writeString(oldVideo, "dummy old content");
        Files.writeString(oldMeta, "{}");
        Files.writeString(recentVideo, "dummy recent content");

        Instant eightDaysAgo = Instant.now().minus(8, ChronoUnit.DAYS);
        Files.setLastModifiedTime(oldVideo, FileTime.from(eightDaysAgo));
        Files.setLastModifiedTime(oldMeta, FileTime.from(eightDaysAgo));

        int deleted = cleanupService.cleanupExportsOlderThan(7);

        assertThat(deleted).isEqualTo(2);
        assertThat(Files.exists(oldVideo)).isFalse();
        assertThat(Files.exists(oldMeta)).isFalse();
        assertThat(Files.exists(recentVideo)).isTrue();
    }

    @Test
    @DisplayName("Elimina temporales de almacenamiento antiguos y preserva los recientes")
    void shouldDeleteTemporaryStorageOlderThanRetentionDays() throws IOException {
        Path oldRender = tempStorageDir.resolve("old_render.mp4");
        Path oldAudio = tempStorageDir.resolve("old_audio.wav");
        Path recentRender = tempStorageDir.resolve("new_render.mp4");

        Files.writeString(oldRender, "mp4");
        Files.writeString(oldAudio, "wav");
        Files.writeString(recentRender, "mp4");

        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        Files.setLastModifiedTime(oldRender, FileTime.from(tenDaysAgo));
        Files.setLastModifiedTime(oldAudio, FileTime.from(tenDaysAgo));

        int deleted = cleanupService.cleanupTemporaryStorageOlderThan(7);

        assertThat(deleted).isEqualTo(2);
        assertThat(Files.exists(oldRender)).isFalse();
        assertThat(Files.exists(oldAudio)).isFalse();
        assertThat(Files.exists(recentRender)).isTrue();
    }

    @Test
    @DisplayName("checkDiskSpace retorna un valor de megabytes utilizables no negativo")
    void checkDiskSpace_ReturnsNonNegativeValue() {
        long usableMb = cleanupService.checkDiskSpace();
        assertThat(usableMb).isGreaterThanOrEqualTo(0);
    }
}
