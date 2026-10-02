package com.storyvideo.api.audio.catalog;

import com.storyvideo.api.domain.StoryGenre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AudioCatalogServiceTest {

    private AudioCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new AudioCatalogService("./target/test-audio");
    }

    @Test
    @DisplayName("Debe resolver nombres de archivo BGM adecuados por género")
    void resolveBgmFileName_ShouldReturnGenreSpecificTracks() {
        assertThat(catalogService.resolveBgmFileName(StoryGenre.HORROR)).contains("horror");
        assertThat(catalogService.resolveBgmFileName(StoryGenre.SCI_FI)).contains("scifi");
        assertThat(catalogService.resolveBgmFileName(StoryGenre.MYSTERY)).contains("mystery");
    }

    @Test
    @DisplayName("Debe resolver rutas para SFX conocidos")
    void resolveSfxPath_ShouldReturnExpectedPath() {
        Path whooshPath = catalogService.resolveSfxPath("WHOOSH");
        assertThat(whooshPath.toString()).contains("whoosh.mp3");

        Path impactPath = catalogService.resolveSfxPath("impact_hit");
        assertThat(impactPath.toString()).contains("impact_hit.mp3");
    }
}
