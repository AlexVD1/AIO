package com.kidsanim.api.audio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AudioCatalogSfxNameTest {

    @Test
    void acceptsValidNames() {
        assertThat(AudioCatalogService.normalizeSfxName("pop")).isEqualTo("pop");
        assertThat(AudioCatalogService.normalizeSfxName(" Big-Number ")).isEqualTo("big_number");
        assertThat(AudioCatalogService.normalizeSfxName("chime.mp3")).isEqualTo("chime");
    }

    @Test
    void rejectsLlmNoise() {
        assertThat(AudioCatalogService.normalizeSfxName("pauseAfterMs: 2500")).isNull();
        assertThat(AudioCatalogService.normalizeSfxName("../etc/passwd")).isNull();
        assertThat(AudioCatalogService.normalizeSfxName("")).isNull();
        assertThat(AudioCatalogService.normalizeSfxName(null)).isNull();
    }
}
