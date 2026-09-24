package com.trivia.api.infrastructure.storage;

import com.trivia.api.service.AssetStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementación de AssetStorageService para almacenamiento en el sistema de archivos local.
 */
@Service
public class LocalAssetStorageService implements AssetStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalAssetStorageService.class);

    private final String storageBasePath;
    private final String storageBaseUrl;

    public LocalAssetStorageService(
            @Value("${trivia.storage.path:./storage}") String storageBasePath,
            @Value("${trivia.storage.base-url:http://localhost:8080/assets}") String storageBaseUrl) {
        this.storageBasePath = storageBasePath;
        this.storageBaseUrl = storageBaseUrl;
    }

    @Override
    public String store(byte[] data, String relativePath, String mimeType) {
        try {
            Path targetPath = Paths.get(storageBasePath, relativePath).normalize();
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.write(targetPath, data);
            log.debug("Asset almacenado localmente en: {}", targetPath.toAbsolutePath());

            String cleanBaseUrl = storageBaseUrl.replaceAll("/+$", "");
            String cleanRelativePath = relativePath.replaceAll("^/+", "");
            return cleanBaseUrl + "/" + cleanRelativePath;
        } catch (IOException e) {
            log.error("Error al guardar asset en {}", relativePath, e);
            throw new IllegalStateException("Error al persistir asset en almacenamiento local: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] retrieve(String relativePath) {
        try {
            Path path = Paths.get(storageBasePath, relativePath).normalize();
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Asset no encontrado: " + relativePath);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("Error al leer asset en {}", relativePath, e);
            throw new IllegalStateException("Error al leer asset local: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        Path path = Paths.get(storageBasePath, relativePath).normalize();
        return Files.exists(path);
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            Path path = Paths.get(storageBasePath, relativePath).normalize();
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el asset en {}", relativePath, e);
            return false;
        }
    }
}
