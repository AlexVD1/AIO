package com.trivia.api.service.video;

import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.TipoTrivia;
import com.trivia.api.domain.Trivia;
import com.trivia.api.service.video.narration.NoOpNarrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpNarrationServiceTest {

    private final NoOpNarrationService service = new NoOpNarrationService(7.0, 5.0, 6.5);

    @Test
    @DisplayName("Debe planificar correctamente el timeline para una lista de trivias")
    void planTimeline_calculaTiemposCorrectos() {
        TipoTrivia tipo = new TipoTrivia("HISTORIA", "Historia", "Eventos");

        Trivia t1 = new Trivia(null, tipo, "Pregunta 1", "pregunta 1", "hash-1", "Explicacion 1", Dificultad.MEDIA, "es-MX", "Roma");
        UUID id1 = UUID.randomUUID();
        ReflectionTestUtils.setField(t1, "id", id1);

        Trivia t2 = new Trivia(null, tipo, "Pregunta 2", "pregunta 2", "hash-2", "Explicacion 2", Dificultad.FACIL, "es-MX", "Grecia");
        UUID id2 = UUID.randomUUID();
        ReflectionTestUtils.setField(t2, "id", id2);

        TimelinePlan plan = service.planTimeline(List.of(t1, t2), VideoFormat.VERTICAL_9_16, true);

        assertThat(plan).isNotNull();
        assertThat(plan.scenes()).hasSize(2);
        assertThat(plan.format()).isEqualTo(VideoFormat.VERTICAL_9_16);
        assertThat(plan.withBgm()).isTrue();

        TriviaSceneTiming s1 = plan.scenes().get(0);
        assertThat(s1.triviaId()).isEqualTo(id1);
        assertThat(s1.index()).isEqualTo(1);
        assertThat(s1.total()).isEqualTo(2);
        assertThat(s1.headerText()).isEqualTo("PREGUNTA 1 DE 2");
        assertThat(s1.questionDuration()).isEqualTo(7.0);
        assertThat(s1.countdownDuration()).isEqualTo(5.0);
        assertThat(s1.answerDuration()).isEqualTo(6.5);
        assertThat(s1.getTotalDuration()).isEqualTo(18.5);

        assertThat(plan.getTotalDuration()).isEqualTo(37.0);
    }
}
