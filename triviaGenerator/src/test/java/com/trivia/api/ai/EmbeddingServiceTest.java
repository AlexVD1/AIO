package com.trivia.api.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

@DisplayName("EmbeddingService — Tests unitarios de vectores y similitud coseno")
class EmbeddingServiceTest {

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingServiceImpl("", new ObjectMapper());
    }

    @Test
    @DisplayName("Vectores idénticos tienen similitud coseno de 1.0")
    void vectoresIdenticosSimilitudUno() {
        float[] v1 = new float[]{1.0f, 2.0f, 3.0f};
        float[] v2 = new float[]{1.0f, 2.0f, 3.0f};

        double similarity = embeddingService.cosineSimilarity(v1, v2);

        assertThat(similarity).isCloseTo(1.0, offset(0.0001));
        assertThat(embeddingService.isSemanticallySimilar(v1, v2, 0.95)).isTrue();
    }

    @Test
    @DisplayName("Vectores ortogonales tienen similitud coseno de 0.0")
    void vectoresOrtogonalesSimilitudCero() {
        float[] v1 = new float[]{1.0f, 0.0f};
        float[] v2 = new float[]{0.0f, 1.0f};

        double similarity = embeddingService.cosineSimilarity(v1, v2);

        assertThat(similarity).isCloseTo(0.0, offset(0.0001));
        assertThat(embeddingService.isSemanticallySimilar(v1, v2, 0.5)).isFalse();
    }

    @Test
    @DisplayName("Vectores opuestos tienen similitud coseno de -1.0")
    void vectoresOpuestosSimilitudMenosUno() {
        float[] v1 = new float[]{1.0f, 0.0f};
        float[] v2 = new float[]{-1.0f, 0.0f};

        double similarity = embeddingService.cosineSimilarity(v1, v2);

        assertThat(similarity).isCloseTo(-1.0, offset(0.0001));
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si las dimensiones de los vectores son distintas")
    void vectoresConDistintaDimensionLanzanExcepcion() {
        float[] v1 = new float[]{1.0f, 2.0f};
        float[] v2 = new float[]{1.0f, 2.0f, 3.0f};

        assertThatThrownBy(() -> embeddingService.cosineSimilarity(v1, v2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incompatibles");
    }

    @Test
    @DisplayName("Serializa y deserializa vectores correctamente")
    void serializacionYDeserializacion() {
        float[] original = new float[]{0.15f, -0.42f, 0.99f};

        String json = embeddingService.serialize(original);
        float[] reconstruido = embeddingService.deserialize(json);

        assertThat(reconstruido).hasSize(3);
        assertThat(reconstruido[0]).isCloseTo(0.15f, offset(0.001f));
        assertThat(reconstruido[1]).isCloseTo(-0.42f, offset(0.001f));
        assertThat(reconstruido[2]).isCloseTo(0.99f, offset(0.001f));
    }

    @Test
    @DisplayName("El fallback local produce un vector normalizado L2")
    void fallbackLocalProduceVectorNormalizado() {
        float[] emb = embeddingService.generateEmbedding("¿Cuál es la distancia a la Luna?");

        assertThat(emb).isNotEmpty();

        double norm = 0.0;
        for (float v : emb) {
            norm += v * v;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, offset(0.001));
    }
}
