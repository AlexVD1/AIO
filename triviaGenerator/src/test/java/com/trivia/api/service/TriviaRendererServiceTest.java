package com.trivia.api.service;

import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.TipoTrivia;
import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaOpcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TriviaRendererService — Tests unitarios de generación de imágenes PNG")
class TriviaRendererServiceTest {

    private TriviaRendererService rendererService;
    private Trivia triviaMock;
    private List<TriviaOpcion> opcionesMock;

    @BeforeEach
    void setUp() {
        rendererService = new TriviaRendererService();

        TipoTrivia tipo = new TipoTrivia("ASTRONOMIA", "Astronomía", "Espacio");
        triviaMock = new Trivia(
                null,
                tipo,
                "¿Cuál es el planeta más grande del sistema solar?",
                "cual es el planeta mas grande del sistema solar",
                "hash-jupiter",
                "Júpiter es un gigante gaseoso con más del doble de masa que todos los demás planetas combinados.",
                Dificultad.FACIL,
                "es-MX",
                "Planetas"
        );

        opcionesMock = List.of(
                new TriviaOpcion(triviaMock, "A", "Saturno", false),
                new TriviaOpcion(triviaMock, "B", "Júpiter", true),
                new TriviaOpcion(triviaMock, "C", "Neptuno", false),
                new TriviaOpcion(triviaMock, "D", "Tierra", false)
        );
    }

    @Test
    @DisplayName("renderPregunta genera un PNG válido de 1080x1080")
    void renderPreguntaGeneraPngValido() throws IOException {
        byte[] bytesPng = rendererService.renderPregunta(triviaMock, opcionesMock);

        assertThat(bytesPng).isNotEmpty();

        // Validar cabecera mágica PNG: 89 50 4E 47 0D 0A 1A 0A
        assertThat(bytesPng[0]).isEqualTo((byte) 0x89);
        assertThat(bytesPng[1]).isEqualTo((byte) 'P');
        assertThat(bytesPng[2]).isEqualTo((byte) 'N');
        assertThat(bytesPng[3]).isEqualTo((byte) 'G');

        // Leer como BufferedImage y verificar dimensiones
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytesPng));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(1080);
        assertThat(image.getHeight()).isEqualTo(1080);
    }

    @Test
    @DisplayName("renderRespuesta genera un PNG válido de 1080x1080")
    void renderRespuestaGeneraPngValido() throws IOException {
        byte[] bytesPng = rendererService.renderRespuesta(triviaMock, opcionesMock);

        assertThat(bytesPng).isNotEmpty();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytesPng));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(1080);
        assertThat(image.getHeight()).isEqualTo(1080);
    }
}
