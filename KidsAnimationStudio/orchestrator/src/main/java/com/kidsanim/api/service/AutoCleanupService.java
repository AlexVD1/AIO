package com.kidsanim.api.service;

import com.kidsanim.api.infrastructure.config.KidsPipelineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Service
public class AutoCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AutoCleanupService.class);

    private final Path storagePath;

    public AutoCleanupService(KidsPipelineProperties pipelineProperties) {
        String base = pipelineProperties != null && pipelineProperties.storagePath() != null
                ? pipelineProperties.storagePath()
                : "./storage";
        this.storagePath = Paths.get(base).toAbsolutePath().normalize();
    }

    public int cleanupTempFiles(int maxAgeHours) {
        Path tempDir = storagePath.resolve("temp");
        if (!Files.exists(tempDir)) {
            return 0;
        }

        Instant cutoff = Instant.now().minus(maxAgeHours, ChronoUnit.HOURS);
        int deletedCount = 0;

        try (Stream<Path> stream = Files.walk(tempDir)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                try {
                    Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                    if (lastModified.isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                        deletedCount++;
                    }
                } catch (Exception ex) {
                    log.debug("No se pudo eliminar temporal {}: {}", path, ex.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Error al escanear directorio temporal {}: {}", tempDir, e.getMessage());
        }

        log.info("Limpieza automática completada: {} archivos eliminados en {}", deletedCount, tempDir);
        return deletedCount;
    }
}
