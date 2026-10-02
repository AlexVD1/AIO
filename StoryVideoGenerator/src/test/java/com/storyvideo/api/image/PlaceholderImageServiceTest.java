package com.storyvideo.api.image;

import com.storyvideo.api.image.dto.GeneratedImage;
import com.storyvideo.api.image.dto.ImageGenerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceholderImageServiceTest {

    private PlaceholderImageService placeholderService;

    @BeforeEach
    void setUp() {
        placeholderService = new PlaceholderImageService();
    }

    @Test
    @DisplayName("Debe generar imagen PNG válida en memoria con dimensiones solicitadas")
    void generate_ShouldReturnValidPng() {
        ImageGenerationRequest request = ImageGenerationRequest.simple(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "cinematic dark shot of abandoned hospital corridor",
                "blurry, cartoon",
                12345L
        );

        GeneratedImage result = placeholderService.generate(request);

        assertThat(result).isNotNull();
        assertThat(result.imageBytes()).isNotEmpty();
        assertThat(result.mimeType()).isEqualTo("image/png");
        assertThat(result.width()).isEqualTo(768);
        assertThat(result.height()).isEqualTo(1344);
        assertThat(result.seedUsed()).isEqualTo(12345L);

        // Verificar 'magic bytes' de cabecera PNG (89 50 4E 47 0D 0A 1A 0A)
        byte[] bytes = result.imageBytes();
        assertThat(bytes[0]).isEqualTo((byte) 0x89);
        assertThat(bytes[1]).isEqualTo((byte) 0x50);
        assertThat(bytes[2]).isEqualTo((byte) 0x4E);
        assertThat(bytes[3]).isEqualTo((byte) 0x47);
    }
}
