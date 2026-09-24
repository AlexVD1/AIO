package com.trivia.api.infrastructure.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalAssetStorageService — Tests unitarios de almacenamiento local")
class LocalAssetStorageServiceTest {

    private Path tempDir;
    private LocalAssetStorageService storageService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("trivia-storage-test");
        storageService = new LocalAssetStorageService(tempDir.toString(), "http://localhost:8080/assets");
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempDir);
    }

    @Test
    @DisplayName("store guarda el archivo en disco y retorna la URL pública correcta")
    void storeGuardaArchivoYRetornaUrl() {
        byte[] data = "contenido simulado png".getBytes();
        String relativePath = "2026/09/trivia123/question.png";

        String url = storageService.store(data, relativePath, "image/png");

        assertThat(url).isEqualTo("http://localhost:8080/assets/2026/09/trivia123/question.png");
        assertThat(storageService.exists(relativePath)).isTrue();

        byte[] leido = storageService.retrieve(relativePath);
        assertThat(leido).isEqualTo(data);
    }

    @Test
    @DisplayName("delete elimina el archivo de disco")
    void deleteEliminaArchivo() {
        byte[] data = "archivo a borrar".getBytes();
        String relativePath = "test/delete-me.png";

        storageService.store(data, relativePath, "image/png");
        assertThat(storageService.exists(relativePath)).isTrue();

        boolean borrado = storageService.delete(relativePath);
        assertThat(borrado).isTrue();
        assertThat(storageService.exists(relativePath)).isFalse();
    }
}
