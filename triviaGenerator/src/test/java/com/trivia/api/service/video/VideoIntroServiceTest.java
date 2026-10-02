package com.trivia.api.service.video;

import com.trivia.api.ai.TriviaAiClient;
import com.trivia.api.domain.Dificultad;
import com.trivia.api.domain.TipoTrivia;
import com.trivia.api.domain.Trivia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoIntroService — Pruebas unitarias de catálogo, resolución y sanitización de introducciones")
class VideoIntroServiceTest {

    @Mock
    private TriviaAiClient aiClient;

    private VideoIntroService service;

    @BeforeEach
    void setUp() {
        service = new VideoIntroService(aiClient);
    }

    @Test
    @DisplayName("Debe contener al menos 7 plantillas en el catálogo inicial")
    void getCatalog_contienePlantillasEsperadas() {
        List<VideoIntroTemplate> catalog = service.getCatalog();
        assertThat(catalog).isNotEmpty();
        assertThat(catalog.size()).isGreaterThanOrEqualTo(7);

        assertThat(catalog)
                .extracting(VideoIntroTemplate::template)
                .anyMatch(t -> t.contains("Pon a prueba tus conocimientos sobre {tema}."))
                .anyMatch(t -> t.contains("¿Qué tanto sabes de {tema}?"))
                .anyMatch(t -> t.contains("¿Cuánto sabes sobre {tema}?"))
                .anyMatch(t -> t.contains("Demuestra cuánto sabes sobre {tema}."));
    }

    @Test
    @DisplayName("Debe resolver el tema explícito si se provee")
    void resolveTopic_explicitoTienePrioridad() {
        String topic = service.resolveTopic("Historia romana", List.of());
        assertThat(topic).isEqualTo("Historia romana");
    }

    @Test
    @DisplayName("Debe inferir el tema del subtema de la primera trivia si no hay tema explícito")
    void resolveTopic_infiereSubtemaDeTrivias() {
        TipoTrivia tipo = new TipoTrivia("HISTORIA", "Historia Universal", "...");
        Trivia t1 = new Trivia(null, tipo, "¿Quién fue Julio César?", "...", "hash1", "...", Dificultad.MEDIA, "es-MX", "Imperio Romano");
        Trivia t2 = new Trivia(null, tipo, "¿En qué año cayó Roma?", "...", "hash2", "...", Dificultad.MEDIA, "es-MX", "Imperio Romano");

        String topic = service.resolveTopic(null, List.of(t1, t2));
        assertThat(topic).isEqualTo("Imperio Romano");
    }

    @Test
    @DisplayName("Debe inferir el nombre del tipo si las trivias no tienen subtema")
    void resolveTopic_infiereNombreTipoSinSubtema() {
        TipoTrivia tipo = new TipoTrivia("CINE", "Cine Clásico", "...");
        Trivia t1 = new Trivia(null, tipo, "¿Quién dirigió Psicosis?", "...", "hash1", "...", Dificultad.MEDIA, "es-MX", null);

        String topic = service.resolveTopic(null, List.of(t1));
        assertThat(topic).isEqualTo("Cine Clásico");
    }

    @Test
    @DisplayName("Debe retornar 'cultura general' como fallback si la lista está vacía")
    void resolveTopic_fallbackCulturaGeneral() {
        String topic = service.resolveTopic(null, List.of());
        assertThat(topic).isEqualTo("cultura general");
    }

    @Test
    @DisplayName("Modalidad NONE debe retornar null sin generar introducción")
    void resolveIntroText_modeNoneRetornaNull() {
        String text = service.resolveIntroText(VideoIntroMode.NONE, "Física", null, null, List.of(), "es-MX");
        assertThat(text).isNull();
    }

    @Test
    @DisplayName("Modalidad TEMPLATE debe reemplazar {tema} correctamente")
    void resolveIntroText_modeTemplateSustituyeTema() {
        String text = service.resolveIntroText(
                VideoIntroMode.TEMPLATE,
                "Interstellar",
                "tpl_que_tanto_sabes",
                null,
                List.of(),
                "es-MX"
        );
        assertThat(text).isEqualTo("¿Qué tanto sabes de Interstellar?");
    }

    @Test
    @DisplayName("Modalidad CUSTOM debe sanitizar comillas y espacios redundantes")
    void resolveIntroText_modeCustomSanitizaTexto() {
        String text = service.resolveIntroText(
                VideoIntroMode.CUSTOM,
                "Interstellar",
                null,
                "  \"¿Eres   realmente un experto  en Interstellar?\"  ",
                List.of(),
                "es-MX"
        );
        assertThat(text).isEqualTo("¿Eres realmente un experto en Interstellar?");
    }

    @Test
    @DisplayName("Modalidad AI debe invocar TriviaAiClient con el tema y retornar la frase")
    void resolveIntroText_modeAiInvocaCliente() {
        when(aiClient.generateVideoIntro("Historia romana", "es-MX"))
                .thenReturn("¿Cuánto sabes realmente sobre el Imperio Romano?");

        String text = service.resolveIntroText(
                VideoIntroMode.AI,
                "Historia romana",
                null,
                null,
                List.of(),
                "es-MX"
        );
        assertThat(text).isEqualTo("¿Cuánto sabes realmente sobre el Imperio Romano?");
    }

    @Test
    @DisplayName("Modalidad AI con fallo debe conmutar a plantilla de respaldo")
    void resolveIntroText_modeAiFalloFallbackResiliente() {
        when(aiClient.generateVideoIntro(anyString(), anyString()))
                .thenThrow(new RuntimeException("Simulated AI error"));

        String text = service.resolveIntroText(
                VideoIntroMode.AI,
                "Física cuántica",
                null,
                null,
                List.of(),
                "es-MX"
        );
        assertThat(text).isNotNull();
        assertThat(text).contains("Física cuántica");
    }
}
