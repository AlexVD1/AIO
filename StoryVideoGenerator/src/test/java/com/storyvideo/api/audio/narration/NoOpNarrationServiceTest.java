package com.storyvideo.api.audio.narration;

import com.storyvideo.api.audio.narration.dto.NarrationRequest;
import com.storyvideo.api.audio.narration.dto.NarrationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpNarrationServiceTest {

    private NoOpNarrationService noOpService;

    @BeforeEach
    void setUp() {
        noOpService = new NoOpNarrationService();
    }

    @Test
    @DisplayName("Debe simular duración proporcional al conteo de palabras")
    void synthesize_ShouldSimulateDuration() {
        NarrationRequest request = NarrationRequest.simple(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "El elevador nunca miente excepto a las tres de la mañana en este viejo hospital.",
                "es-MX-JorgeNeural"
        );

        NarrationResult result = noOpService.synthesize(request);

        assertThat(result).isNotNull();
        assertThat(result.audioBytes()).isNotEmpty();
        assertThat(result.durationSeconds()).isGreaterThan(4.0);
        assertThat(result.audioFormat()).isEqualTo("audio/mpeg");
        assertThat(result.voiceUsed()).isEqualTo("simulated-voice");
    }

    @Test
    @DisplayName("Debe reportarse siempre como disponible")
    void isAvailable_ShouldReturnTrue() {
        assertThat(noOpService.isAvailable()).isTrue();
    }
}
