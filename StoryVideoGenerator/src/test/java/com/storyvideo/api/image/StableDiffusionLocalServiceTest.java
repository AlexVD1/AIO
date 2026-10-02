package com.storyvideo.api.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.image.dto.GeneratedImage;
import com.storyvideo.api.image.dto.ImageGenerationRequest;
import com.storyvideo.api.image.exception.ImageGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StableDiffusionLocalServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ObjectMapper objectMapper;
    private StableDiffusionLocalService sdService;

    // 1x1 pixel PNG válido codificado en Base64
    private static final String BASE64_TEST_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sdService = new StableDiffusionLocalService(
                "http://localhost:7860",
                30,
                objectMapper,
                httpClient
        );
    }

    @Test
    @DisplayName("Debe deserializar respuesta Base64 de Forge API y retornar GeneratedImage")
    void generate_Success() throws Exception {
        String forgeResponseJson = """
                {
                  "images": ["%s"],
                  "parameters": {
                    "prompt": "test prompt",
                    "seed": 987654321
                  },
                  "info": "{\\"seed\\": 987654321}"
                }
                """.formatted(BASE64_TEST_PNG);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(forgeResponseJson);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        ImageGenerationRequest request = ImageGenerationRequest.simple(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "cinematic photo",
                "bad quality",
                987654321L
        );

        GeneratedImage result = sdService.generate(request);

        assertThat(result).isNotNull();
        assertThat(result.imageBytes()).isNotEmpty();
        assertThat(result.mimeType()).isEqualTo("image/png");
        assertThat(result.seedUsed()).isEqualTo(987654321L);
        assertThat(result.width()).isEqualTo(768);
        assertThat(result.height()).isEqualTo(1344);
    }

    @Test
    @DisplayName("Debe lanzar ImageGenerationException ante código HTTP distinto de 200")
    void generate_HttpError_ThrowsException() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error: CUDA out of memory");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        ImageGenerationRequest request = ImageGenerationRequest.simple(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "cinematic photo",
                "bad quality",
                123L
        );

        assertThatThrownBy(() -> sdService.generate(request))
                .isInstanceOf(ImageGenerationException.class)
                .hasMessageContaining("HTTP 500");
    }
}
