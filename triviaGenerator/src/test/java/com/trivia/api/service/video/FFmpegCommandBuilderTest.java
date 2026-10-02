package com.trivia.api.service.video;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FFmpegCommandBuilderTest {

    private final FFmpegCommandBuilder builder = new FFmpegCommandBuilder();

    private final VideoAssetExtractor.ResolvedAssets dummyAssets = new VideoAssetExtractor.ResolvedAssets(
            "/assets/tick.wav",
            "/assets/correct.wav",
            "/assets/whoosh.wav",
            "/assets/bgm.wav",
            "/assets/font.ttf"
    );

    @Test
    @DisplayName("Debe construir comando FFmpeg válido para formato vertical (9:16) sin TTS")
    void buildSegmentCommand_verticalFormat() {
        TriviaSceneTiming timing = new TriviaSceneTiming(
                UUID.randomUUID(),
                1,
                10,
                "PREGUNTA 1 DE 10",
                4.5,
                5.0,
                3.5,
                null,
                null
        );

        List<String> cmd = builder.buildSegmentCommand(
                "/tmp/q.png",
                "/tmp/a.png",
                timing,
                VideoFormat.VERTICAL_9_16,
                dummyAssets,
                "/tmp/out.mp4"
        );

        String fullCmd = String.join(" ", cmd);
        assertThat(fullCmd)
                .contains("-loop 1 -t 9.50 -i /tmp/q.png")
                .contains("-loop 1 -t 3.50 -i /tmp/a.png")
                .contains("1080x1920")
                .contains("libx264")
                .contains("aac")
                .contains("amix=inputs=8")
                .contains("normalize=0")
                .contains("alimiter=limit=0.95")
                .contains("/tmp/out.mp4");
    }

    @Test
    @DisplayName("Debe construir comando FFmpeg válido para formato panorámico horizontal (16:9) para YouTube")
    void buildSegmentCommand_horizontalFormat() {
        TriviaSceneTiming timing = new TriviaSceneTiming(
                UUID.randomUUID(),
                1,
                5,
                "PREGUNTA 1 DE 5",
                4.5,
                5.0,
                3.5,
                null,
                null
        );

        List<String> cmd = builder.buildSegmentCommand(
                "/tmp/q.png",
                "/tmp/a.png",
                timing,
                VideoFormat.HORIZONTAL_16_9,
                dummyAssets,
                "/tmp/out_16_9.mp4"
        );

        String fullCmd = String.join(" ", cmd);
        assertThat(fullCmd)
                .contains("-loop 1 -t 9.50 -i /tmp/q.png")
                .contains("-loop 1 -t 3.50 -i /tmp/a.png")
                .contains("1920x1080")
                .contains("overlay=420:0")
                .contains("TRIVIA QUIZ")
                .contains("RESPUESTA CORRECTA")
                .contains("libx264")
                .contains("/tmp/out_16_9.mp4");
    }

    @Test
    @DisplayName("Debe construir comando FFmpeg con pistas de voz TTS cuando están presentes")
    void buildSegmentCommand_conTts() {
        TriviaSceneTiming timing = new TriviaSceneTiming(
                UUID.randomUUID(),
                1,
                10,
                "PREGUNTA 1 DE 10",
                7.0,
                5.0,
                6.5,
                "/tmp/voice_q.mp3",
                "/tmp/voice_a.mp3"
        );

        List<String> cmd = builder.buildSegmentCommand(
                "/tmp/q.png",
                "/tmp/a.png",
                timing,
                VideoFormat.VERTICAL_9_16,
                dummyAssets,
                "/tmp/out_tts.mp4"
        );

        String fullCmd = String.join(" ", cmd);
        assertThat(fullCmd)
                .contains("-i /tmp/voice_q.mp3")
                .contains("-i /tmp/voice_a.mp3")
                .contains("a_voice_q")
                .contains("a_voice_a")
                .contains("amix=inputs=10")
                .contains("normalize=0")
                .contains("alimiter=limit=0.95")
                .contains("/tmp/out_tts.mp4");
    }

    @Test
    @DisplayName("Debe construir comando de concatenación y mezcla de BGM")
    void buildConcatAndBgmCommand() {
        List<String> concatCmd = builder.buildConcatCommand("/tmp/list.txt", "/tmp/raw.mp4");
        assertThat(concatCmd).contains("-f", "concat", "-i", "/tmp/list.txt", "-c", "copy", "/tmp/raw.mp4");

        List<String> bgmCmd = builder.buildBgmCommand("/tmp/raw.mp4", "/tmp/bgm.wav", 130.0, 0.15, "/tmp/final.mp4");
        assertThat(bgmCmd).contains("-stream_loop", "-1", "-i", "/tmp/bgm.wav", "-filter_complex");
        assertThat(String.join(" ", bgmCmd)).contains("normalize=0").contains("alimiter=limit=0.95");
        assertThat(bgmCmd.get(bgmCmd.size() - 1)).isEqualTo("/tmp/final.mp4");
    }

    @Test
    @DisplayName("Debe construir comando FFmpeg para escena de introducción vertical sin TTS")
    void buildIntroSegmentCommand_verticalSinTts() {
        VideoIntroTiming intro = new VideoIntroTiming(
                "¿Qué tanto sabes de Historia romana?",
                "Historia romana",
                4.0,
                null
        );

        List<String> cmd = builder.buildIntroSegmentCommand(
                "/tmp/intro.png",
                intro,
                VideoFormat.VERTICAL_9_16,
                dummyAssets,
                "/tmp/intro_seg.mp4"
        );

        String fullCmd = String.join(" ", cmd);
        assertThat(fullCmd)
                .contains("-loop 1 -t 4.00 -i /tmp/intro.png")
                .contains("-i /assets/whoosh.wav")
                .contains("1080x1920")
                .contains("¡NUEVA TRIVIA!")
                .contains("amix=inputs=2")
                .contains("normalize=0")
                .contains("alimiter=limit=0.95")
                .contains("/tmp/intro_seg.mp4");
    }

    @Test
    @DisplayName("Debe construir comando FFmpeg para escena de introducción con voz TTS")
    void buildIntroSegmentCommand_conTts() {
        VideoIntroTiming intro = new VideoIntroTiming(
                "Pon a prueba tus conocimientos sobre Interstellar.",
                "Interstellar",
                3.8,
                "/tmp/intro_tts.mp3"
        );

        List<String> cmd = builder.buildIntroSegmentCommand(
                "/tmp/intro.png",
                intro,
                VideoFormat.VERTICAL_9_16,
                dummyAssets,
                "/tmp/intro_seg_tts.mp4"
        );

        String fullCmd = String.join(" ", cmd);
        assertThat(fullCmd)
                .contains("-i /tmp/intro_tts.mp3")
                .contains("a_voice_intro")
                .contains("amix=inputs=3")
                .contains("normalize=0")
                .contains("alimiter=limit=0.95")
                .contains("/tmp/intro_seg_tts.mp4");
    }

    @Test
    @DisplayName("Debe construir comando FFmpeg para intro en formato horizontal (16:9) y cuadrado (1:1)")
    void buildIntroSegmentCommand_otrosFormatos() {
        VideoIntroTiming intro = new VideoIntroTiming("Trivia Quiz", "Cine", 3.5, null);

        List<String> cmdH = builder.buildIntroSegmentCommand("/tmp/intro.png", intro, VideoFormat.HORIZONTAL_16_9, dummyAssets, "/tmp/h.mp4");
        assertThat(String.join(" ", cmdH)).contains("1920x1080").contains("overlay=420:0");

        List<String> cmdSq = builder.buildIntroSegmentCommand("/tmp/intro.png", intro, VideoFormat.SQUARE_1_1, dummyAssets, "/tmp/sq.mp4");
        assertThat(String.join(" ", cmdSq)).contains("scale=1080:1080");
    }
}
