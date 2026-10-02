package com.storyvideo.api.video.editing.service;

import com.storyvideo.api.domain.StoryGenre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicEditingServiceTest {

    private DynamicEditingService editingService;

    @BeforeEach
    void setUp() {
        editingService = new DynamicEditingService();
    }

    @Test
    @DisplayName("Genera filtros visuales atmosféricos para HORROR (vignette y grain)")
    void shouldResolveHorrorEffects() {
        String filter = editingService.resolveVisualEffectsFilter(StoryGenre.HORROR, List.of());
        assertThat(filter).contains("vignette");
        assertThat(filter).contains("noise");
    }

    @Test
    @DisplayName("Genera aberración cromática para SCI_FI")
    void shouldResolveSciFiEffects() {
        String filter = editingService.resolveVisualEffectsFilter(StoryGenre.SCI_FI, List.of());
        assertThat(filter).contains("rgbashift");
    }

    @Test
    @DisplayName("Añade efectos personalizados especificados en la escena")
    void shouldIncludeCustomSceneEffects() {
        String filter = editingService.resolveVisualEffectsFilter(StoryGenre.MYSTERY, List.of("GLITCH", "BLUR"));
        assertThat(filter).contains("rgbashift");
        assertThat(filter).contains("boxblur");
    }
}
