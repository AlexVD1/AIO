package com.trivia.api.service.video;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Constructor de comandos y filtergraphs complejos para FFmpeg.
 */
@Component
public class FFmpegCommandBuilder {

    /**
     * Construye el comando de FFmpeg para renderizar el segmento de una trivia individual.
     */
    public List<String> buildSegmentCommand(
            String questionImagePath,
            String answerImagePath,
            TriviaSceneTiming timing,
            VideoFormat format,
            VideoAssetExtractor.ResolvedAssets assets,
            String outputSegmentPath) {

        List<String> args = new ArrayList<>();
        args.add("-y");

        double qDuration = timing.questionDuration() + timing.countdownDuration();
        double aDuration = timing.answerDuration();
        double totalDuration = timing.getTotalDuration();

        // Inputs
        // [0] Pregunta
        args.add("-loop");
        args.add("1");
        args.add("-t");
        args.add(String.format(Locale.US, "%.2f", qDuration));
        args.add("-i");
        args.add(questionImagePath);

        // [1] Respuesta
        args.add("-loop");
        args.add("1");
        args.add("-t");
        args.add(String.format(Locale.US, "%.2f", aDuration));
        args.add("-i");
        args.add(answerImagePath);

        // [2] SFX Whoosh
        args.add("-i");
        args.add(assets.whooshAudioPath());

        // [3] SFX Tick
        args.add("-i");
        args.add(assets.tickAudioPath());

        // [4] SFX Correct Chime
        args.add("-i");
        args.add(assets.correctAudioPath());

        boolean hasTts = timing.questionAudioPath() != null && !timing.questionAudioPath().isBlank()
                && timing.answerAudioPath() != null && !timing.answerAudioPath().isBlank();

        if (hasTts) {
            // [5] TTS Pregunta
            args.add("-i");
            args.add(timing.questionAudioPath());
            // [6] TTS Respuesta
            args.add("-i");
            args.add(timing.answerAudioPath());
        }

        // Filtros de video según el formato
        String font = assets.fontPath();
        String videoFilter;

        double countdownStart = timing.questionDuration();
        double countdownEnd = countdownStart + timing.countdownDuration() - 0.01;
        double revealTime = qDuration;

        if (format == VideoFormat.VERTICAL_9_16) {
            videoFilter = String.format(Locale.US,
                    "color=c=0x0F172A:s=1080x1920:d=%.2f[bg0];" +
                    "[0:v]scale=1080:1080[qv0];" +
                    "[bg0][qv0]overlay=0:420[qbase];" +
                    "[qbase]drawtext=fontfile='%s':text='%s':fontsize=42:fontcolor=0x38BDF8:x=(w-text_w)/2:y=180[qhead];" +
                    "[qhead]drawtext=fontfile='%s':text='TIEMPO  %%{eif\\:%.0f-t\\:d}':fontsize=52:fontcolor=white:box=1:boxcolor=0x1E293BEE:boxborderw=16:x=(w-text_w)/2:y=280:enable='between(t,%.2f,%.2f)'[v0];" +
                    "color=c=0x0F172A:s=1080x1920:d=%.2f[bg1];" +
                    "[1:v]scale=1080:1080[av0];" +
                    "[bg1][av0]overlay=0:420[abase];" +
                    "[abase]drawtext=fontfile='%s':text='%s':fontsize=42:fontcolor=0x38BDF8:x=(w-text_w)/2:y=180[v1];" +
                    "[v0][v1]concat=n=2:v=1:a=0[vfinal];",
                    qDuration,
                    font, timing.headerText(),
                    font, countdownEnd + 0.5, countdownStart, countdownEnd,
                    aDuration,
                    font, timing.headerText()
            );
        } else if (format == VideoFormat.HORIZONTAL_16_9) {
            videoFilter = String.format(Locale.US,
                    "color=c=0x0F172A:s=1920x1080:d=%.2f[bg0];" +
                    "[0:v]scale=1080:1080[qv0];" +
                    "[bg0][qv0]overlay=420:0[qbase];" +
                    "[qbase]drawtext=fontfile='%s':text='%s':fontsize=36:fontcolor=0x38BDF8:x=(420-text_w)/2:y=200[qhead];" +
                    "[qhead]drawtext=fontfile='%s':text='TRIVIA QUIZ':fontsize=24:fontcolor=0x94A3B8:x=(420-text_w)/2:y=260[qsub];" +
                    "[qsub]drawtext=fontfile='%s':text='TIEMPO':fontsize=32:fontcolor=0x94A3B8:x=1500+(420-text_w)/2:y=440:enable='between(t,%.2f,%.2f)'[qtimerlabel];" +
                    "[qtimerlabel]drawtext=fontfile='%s':text='%%{eif\\:%.0f-t\\:d}':fontsize=76:fontcolor=white:box=1:boxcolor=0x1E293BEE:boxborderw=20:x=1500+(420-text_w)/2:y=500:enable='between(t,%.2f,%.2f)'[v0];" +
                    "color=c=0x0F172A:s=1920x1080:d=%.2f[bg1];" +
                    "[1:v]scale=1080:1080[av0];" +
                    "[bg1][av0]overlay=420:0[abase];" +
                    "[abase]drawtext=fontfile='%s':text='%s':fontsize=36:fontcolor=0x38BDF8:x=(420-text_w)/2:y=200[ahead];" +
                    "[ahead]drawtext=fontfile='%s':text='TRIVIA QUIZ':fontsize=24:fontcolor=0x94A3B8:x=(420-text_w)/2:y=260[asub];" +
                    "[asub]drawtext=fontfile='%s':text='RESPUESTA CORRECTA':fontsize=26:fontcolor=0x10B981:box=1:boxcolor=0x064E3BEE:boxborderw=14:x=1500+(420-text_w)/2:y=480[v1];" +
                    "[v0][v1]concat=n=2:v=1:a=0[vfinal];",
                    qDuration,
                    font, timing.headerText(),
                    font,
                    font, countdownStart, countdownEnd,
                    font, countdownEnd + 0.5, countdownStart, countdownEnd,
                    aDuration,
                    font, timing.headerText(),
                    font,
                    font
            );
        } else {
            // SQUARE_1_1 (1080x1080)
            videoFilter = String.format(Locale.US,
                    "[0:v]scale=1080:1080,setsar=1[v0_raw];" +
                    "[1:v]scale=1080:1080,setsar=1[v1_raw];" +
                    "[v0_raw]drawtext=fontfile='%s':text='TIEMPO  %%{eif\\:%.0f-t\\:d}':fontsize=52:fontcolor=white:box=1:boxcolor=0x0F172ACC:boxborderw=16:x=(w-text_w)/2:y=920:enable='between(t,%.2f,%.2f)'[v0];" +
                    "[v1_raw]null[v1];" +
                    "[v0][v1]concat=n=2:v=1:a=0[vfinal];",
                    font, countdownEnd + 0.5, countdownStart, countdownEnd
            );
        }

        // Filtros de audio (sincronización de efectos de sonido y voz)
        // Delays en milisegundos
        long tick1Ms = (long) (countdownStart * 1000);
        long tick2Ms = tick1Ms + 1000;
        long tick3Ms = tick1Ms + 2000;
        long tick4Ms = tick1Ms + 3000;
        long tick5Ms = tick1Ms + 4000;
        long chimeMs = (long) (revealTime * 1000);

        String audioFilter;
        if (hasTts) {
            long qVoiceMs = 400;
            long aVoiceMs = chimeMs + 400;
            audioFilter = String.format(Locale.US,
                    "aevalsrc=0:d=%.2f[asilence];" +
                    "[2:a]adelay=0|0[a_whoosh];" +
                    "[5:a]adelay=%d|%d,volume=1.4[a_voice_q];" +
                    "[3:a]asplit=5[t1][t2][t3][t4][t5];" +
                    "[t1]adelay=%d|%d[at1];" +
                    "[t2]adelay=%d|%d[at2];" +
                    "[t3]adelay=%d|%d[at3];" +
                    "[t4]adelay=%d|%d[at4];" +
                    "[t5]adelay=%d|%d[at5];" +
                    "[4:a]adelay=%d|%d[a_correct];" +
                    "[6:a]adelay=%d|%d,volume=1.4[a_voice_a];" +
                    "[asilence][a_whoosh][a_voice_q][at1][at2][at3][at4][at5][a_correct][a_voice_a]amix=inputs=10:duration=first:dropout_transition=0,volume=1.8[afinal]",
                    totalDuration,
                    qVoiceMs, qVoiceMs,
                    tick1Ms, tick1Ms,
                    tick2Ms, tick2Ms,
                    tick3Ms, tick3Ms,
                    tick4Ms, tick4Ms,
                    tick5Ms, tick5Ms,
                    chimeMs, chimeMs,
                    aVoiceMs, aVoiceMs
            );
        } else {
            audioFilter = String.format(Locale.US,
                    "aevalsrc=0:d=%.2f[asilence];" +
                    "[2:a]adelay=0|0[a_whoosh];" +
                    "[3:a]asplit=5[t1][t2][t3][t4][t5];" +
                    "[t1]adelay=%d|%d[at1];" +
                    "[t2]adelay=%d|%d[at2];" +
                    "[t3]adelay=%d|%d[at3];" +
                    "[t4]adelay=%d|%d[at4];" +
                    "[t5]adelay=%d|%d[at5];" +
                    "[4:a]adelay=%d|%d[a_correct];" +
                    "[asilence][a_whoosh][at1][at2][at3][at4][at5][a_correct]amix=inputs=8:duration=first:dropout_transition=0,volume=1.8[afinal]",
                    totalDuration,
                    tick1Ms, tick1Ms,
                    tick2Ms, tick2Ms,
                    tick3Ms, tick3Ms,
                    tick4Ms, tick4Ms,
                    tick5Ms, tick5Ms,
                    chimeMs, chimeMs
            );
        }

        args.add("-filter_complex");
        args.add(videoFilter + audioFilter);

        args.add("-map");
        args.add("[vfinal]");
        args.add("-map");
        args.add("[afinal]");

        args.add("-c:v");
        args.add("libx264");
        args.add("-preset");
        args.add("veryfast");
        args.add("-pix_fmt");
        args.add("yuv420p");
        args.add("-r");
        args.add("30");

        args.add("-c:a");
        args.add("aac");
        args.add("-b:a");
        args.add("192k");

        args.add("-t");
        args.add(String.format(Locale.US, "%.2f", totalDuration));

        args.add(outputSegmentPath);
        return args;
    }

    /**
     * Construye el comando para concatenar segmentos sin pérdida mediante el demuxer concat.
     */
    public List<String> buildConcatCommand(String concatListFilePath, String outputConcatPath) {
        List<String> args = new ArrayList<>();
        args.add("-y");
        args.add("-f");
        args.add("concat");
        args.add("-safe");
        args.add("0");
        args.add("-i");
        args.add(concatListFilePath);
        args.add("-c");
        args.add("copy");
        args.add(outputConcatPath);
        return args;
    }

    /**
     * Construye el comando para mezclar la música de fondo (BGM) en bucle con el audio de los segmentos.
     */
    public List<String> buildBgmCommand(
            String rawConcatVideoPath,
            String bgmAudioPath,
            double totalDuration,
            double bgmVolume,
            String finalOutputPath) {

        List<String> args = new ArrayList<>();
        args.add("-y");
        args.add("-i");
        args.add(rawConcatVideoPath);
        args.add("-stream_loop");
        args.add("-1");
        args.add("-i");
        args.add(bgmAudioPath);

        args.add("-filter_complex");
        args.add(String.format(Locale.US,
                "[1:a]volume=%.2f[bgm_soft];" +
                "[0:a][bgm_soft]amix=inputs=2:duration=first:dropout_transition=0[aout]",
                bgmVolume
        ));

        args.add("-map");
        args.add("0:v");
        args.add("-map");
        args.add("[aout]");
        args.add("-c:v");
        args.add("copy");
        args.add("-c:a");
        args.add("aac");
        args.add("-b:a");
        args.add("192k");
        args.add("-t");
        args.add(String.format(Locale.US, "%.2f", totalDuration));
        args.add(finalOutputPath);
        return args;
    }
}
