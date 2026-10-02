package com.storyvideo.api.export.service;

import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AutoCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AutoCleanupService.class);

    private final Path exportBasePath;
    private final Path storageBasePath;
    private final int retentionDays;
    private final long minFreeDiskMb;
    private final boolean cleanupEnabled;

    public AutoCleanupService(
            @Value("${story.export.path:./export_videos}") String exportPath,
            @Value("${story.storage.path:./storage}") String storagePath,
            @Value("${story.cleanup.retention-days:7}") int retentionDays,
            @Value("${story.cleanup.min-free-disk-mb:1024}") long minFreeDiskMb,
            @Value("${story.cleanup.enabled:true}") boolean cleanupEnabled) {
        this.exportBasePath = Paths.get(exportPath).toAbsolutePath().normalize();
        this.storageBasePath = Paths.get(storagePath).toAbsolutePath().normalize();
        this.retentionDays = retentionDays;
        this.minFreeDiskMb = minFreeDiskMb;
        this.cleanupEnabled = cleanupEnabled;
    }

    @Scheduled(cron = "${story.cleanup.cron:0 0 3 * * ?}")
    public void runScheduledCleanup() {
        if (!cleanupEnabled) {
            log.info("Limpieza automática deshabilitada por configuración");
            return;
        }

        log.info("Iniciando rutina de limpieza automática de exports y archivos temporales (Retención: {} días)", retentionDays);
        checkDiskSpace();
        cleanupExportsOlderThan(retentionDays);
        cleanupTemporaryStorageOlderThan(retentionDays);
    }

    public long checkDiskSpace() {
        File partition = storageBasePath.toFile();
        if (!partition.exists()) {
            partition = exportBasePath.toFile();
        }
        if (!partition.exists()) {
            partition = new File(".");
        }

        long usableMb = partition.getUsableSpace() / (1024 * 1024);
        if (usableMb < minFreeDiskMb) {
            log.warn("ALERTA DE ESPACIO EN DISCO: Espacio libre utilizable es de sólo {} MB (Mínimo requerido: {} MB)",
                    usableMb, minFreeDiskMb);
        } else {
            log.info("Verificación de espacio en disco: {} MB disponibles", usableMb);
        }
        return usableMb;
    }

    public int cleanupExportsOlderThan(int days) {
        return deleteOldFilesRecursively(exportBasePath, days, false);
    }

    public int cleanupTemporaryStorageOlderThan(int days) {
        return deleteOldFilesRecursively(storageBasePath, days, true);
    }

    private int deleteOldFilesRecursively(Path dir, int days, boolean tempOnly) {
        if (!Files.exists(dir)) {
            return 0;
        }

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        AtomicInteger deletedFiles = new AtomicInteger(0);
        AtomicLong freedBytes = new AtomicLong(0);

        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        String name = file.getFileName().toString().toLowerCase();
                        // En almacenamiento temporal limpiamos mp4, wav, mp3, png, srt antiguos
                        boolean shouldDelete = !tempOnly || name.endsWith(".mp4") || name.endsWith(".wav")
                                || name.endsWith(".mp3") || name.endsWith(".png") || name.endsWith(".srt");

                        if (shouldDelete) {
                            long size = attrs.size();
                            Files.deleteIfExists(file);
                            deletedFiles.incrementAndGet();
                            freedBytes.addAndGet(size);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("Limpieza en {}: {} archivos eliminados, {} MB liberados",
                    dir.getFileName(), deletedFiles.get(), freedBytes.get() / (1024 * 1024));

        } catch (IOException e) {
            log.error("Error durante la limpieza recursiva en {}: {}", dir, e.getMessage());
        }

        return deletedFiles.get();
    }
}
