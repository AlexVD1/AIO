package com.kidsanim.api.video.ffmpeg;

import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.video.timeline.model.SfxCue;
import com.kidsanim.api.video.timeline.model.TimelineEntry;
import com.kidsanim.api.video.timeline.model.VideoTimeline;
import com.kidsanim.api.video.timeline.model.VolumeDuckPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FFmpegCommandBuilder {

    private static final Logger log = LoggerFactory.getLogger(FFmpegCommandBuilder.class);

    private final String ffmpegPath;
    private final int defaultWidth;
    private final int defaultHeight;
    private final int defaultFps;
    private final String defaultEncoder;
    private final String defaultPreset;
    private final int defaultCrf;

    public FFmpegCommandBuilder(KidsVideoProperties videoProperties) {
        this.ffmpegPath = videoProperties != null && videoProperties.ffmpegPath() != null
                ? videoProperties.ffmpegPath()
                : "ffmpeg";
        this.defaultWidth = videoProperties != null && videoProperties.width() != null ? videoProperties.width() : 1920;
        this.defaultHeight = videoProperties != null && videoProperties.height() != null ? videoProperties.height() : 1080;
        this.defaultFps = videoProperties != null && videoProperties.fps() != null ? videoProperties.fps() : 30;
        this.defaultEncoder = videoProperties != null && videoProperties.encoder() != null ? videoProperties.encoder() : "h264_nvenc";
        this.defaultPreset = videoProperties != null && videoProperties.preset() != null ? videoProperties.preset() : "medium";
        this.defaultCrf = videoProperties != null && videoProperties.crf() != null ? videoProperties.crf() : 22;
    }

    public List<String> buildRenderCommand(VideoTimeline timeline, Path outputPath, Path subtitleAssPath) {
        return buildRenderCommandWithEncoder(timeline, outputPath, subtitleAssPath, defaultEncoder);
    }

    public List<String> buildRenderCommandWithEncoder(VideoTimeline timeline, Path outputPath, Path subtitleAssPath, String encoder) {
        if (timeline == null || timeline.entries() == null || timeline.entries().isEmpty()) {
            throw new IllegalArgumentException("No se puede generar comando FFmpeg para un timeline vacío");
        }

        int width = timeline.width() > 0 ? timeline.width() : defaultWidth;
        int height = timeline.height() > 0 ? timeline.height() : defaultHeight;
        int fps = timeline.fps() > 0 ? timeline.fps() : defaultFps;
        String chosenEncoder = encoder != null && !encoder.isBlank() ? encoder : defaultEncoder;

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-hide_banner");
        command.add("-y");

        List<String> videoFilterNodes = new ArrayList<>();
        List<String> audioFilterNodes = new ArrayList<>();
        List<String> audioMixInputs = new ArrayList<>();

        int currentInputIndex = 0;

        // 1. Inputs de Video (Clips o Keyframes)
        for (int i = 0; i < timeline.entries().size(); i++) {
            TimelineEntry entry = timeline.entries().get(i);
            double duration = entry.duration();
            int frames = (int) Math.max(1, Math.round(duration * fps));

            boolean hasClip = entry.clipPath() != null && new File(entry.clipPath()).exists();
            boolean hasImage = entry.keyframeImagePath() != null && new File(entry.keyframeImagePath()).exists();

            // Detectar transiciones suaves de escena: fade in al inicio de escena y fade out al final
            boolean isSceneStart = i > 0 && entry.sceneOrderIndex() > timeline.entries().get(i - 1).sceneOrderIndex();
            boolean isSceneEnd = i < timeline.entries().size() - 1 && entry.sceneOrderIndex() < timeline.entries().get(i + 1).sceneOrderIndex();

            String transitionFilter = "";
            if (isSceneStart) {
                transitionFilter += ",fade=t=in:st=0:d=0.30";
            }
            if (isSceneEnd) {
                double fadeOutStart = Math.max(0.0, duration - 0.30);
                transitionFilter += String.format(Locale.US, ",fade=t=out:st=%.2f:d=0.30", fadeOutStart);
            }

            if (hasClip) {
                // Entrada de clip de video
                command.add("-i");
                command.add(entry.clipPath());

                String videoFilter = String.format(Locale.US,
                        "[%d:v]scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=%d,trim=duration=%.2f,setpts=PTS-STARTPTS%s[v%d]",
                        currentInputIndex, width, height, width, height, fps, duration, transitionFilter, i);
                videoFilterNodes.add(videoFilter);
            } else if (hasImage) {
                // Entrada de imagen en loop con zoom sutil infantil
                command.add("-loop");
                command.add("1");
                command.add("-t");
                command.add(String.format(Locale.US, "%.2f", duration));
                command.add("-i");
                command.add(entry.keyframeImagePath());

                String zoomExpr = resolveZoomExpression(entry.cameraMotion(), frames);
                String videoFilter = String.format(Locale.US,
                        "[%d:v]scale=2160:1215,zoompan=%s:d=%d:s=%dx%d:fps=%d,trim=duration=%.2f,setpts=PTS-STARTPTS%s[v%d]",
                        currentInputIndex, zoomExpr, frames, width, height, fps, duration, transitionFilter, i);
                videoFilterNodes.add(videoFilter);
            } else {
                // Fallback lavfi si no hay archivo físico (ej. fondo pastel)
                command.add("-f");
                command.add("lavfi");
                command.add("-t");
                command.add(String.format(Locale.US, "%.2f", duration));
                command.add("-i");
                command.add(String.format("color=c=0x2c3e50:s=%dx%d:r=%d", width, height, fps));

                String videoFilter = String.format(Locale.US,
                        "[%d:v]setpts=PTS-STARTPTS%s[v%d]",
                        currentInputIndex, transitionFilter, i);
                videoFilterNodes.add(videoFilter);
            }

            currentInputIndex++;
        }

        // 2. Concatenación de video
        StringBuilder concatFilter = new StringBuilder();
        for (int i = 0; i < timeline.entries().size(); i++) {
            concatFilter.append(String.format("[v%d]", i));
        }

        boolean hasSubtitles = subtitleAssPath != null && Files.exists(subtitleAssPath);
        String voutTarget = hasSubtitles ? "[vout_raw]" : "[vout]";
        concatFilter.append(String.format("concat=n=%d:v=1:a=0%s", timeline.entries().size(), voutTarget));
        videoFilterNodes.add(concatFilter.toString());

        // Quemado de subtítulos y overlays ASS
        if (hasSubtitles) {
            String safeSubPath = subtitleAssPath.toAbsolutePath().toString().replace("\\", "/").replace(":", "\\:");
            String subtitleFilter = String.format(Locale.US, "[vout_raw]ass='%s'[vout]", safeSubPath);
            videoFilterNodes.add(subtitleFilter);
        }

        // 3. Audio base: Silencio estéreo para evitar desajustes
        double totalDuration = timeline.totalDurationSeconds();
        audioFilterNodes.add(String.format(Locale.US, "aevalsrc=0:d=%.2f:s=44100:c=stereo[asilence]", totalDuration));
        audioMixInputs.add("[asilence]");

        // 4. Inputs de Narración por plano
        for (int i = 0; i < timeline.entries().size(); i++) {
            TimelineEntry entry = timeline.entries().get(i);
            if (entry.narrationAudioPath() != null && new File(entry.narrationAudioPath()).exists()) {
                command.add("-i");
                command.add(entry.narrationAudioPath());

                long delayMs = Math.round((entry.startTime() + entry.narrationStartOffset()) * 1000.0);
                String narrFilter = String.format(Locale.US,
                        "[%d:a]adelay=%d|%d,volume=1.0[anarr_%d]",
                        currentInputIndex, delayMs, delayMs, i);
                audioFilterNodes.add(narrFilter);
                audioMixInputs.add(String.format("[anarr_%d]", i));

                currentInputIndex++;
            }
        }

        // 5. Input de Música de Fondo (BGM) con Ducking
        if (timeline.audioMix() != null && timeline.audioMix().musicTrackPath() != null
                && new File(timeline.audioMix().musicTrackPath()).exists()) {
            command.add("-stream_loop");
            command.add("-1");
            command.add("-i");
            command.add(timeline.audioMix().musicTrackPath());

            double baseVolume = timeline.audioMix().musicBaseVolume();
            String duckingExpr = buildDuckingVolumeExpression(timeline.audioMix().duckingPoints(), baseVolume);

            String bgmFilter = String.format(Locale.US,
                    "[%d:a]atrim=0:%.2f,%s[abgm]",
                    currentInputIndex, totalDuration, duckingExpr);
            audioFilterNodes.add(bgmFilter);
            audioMixInputs.add("[abgm]");

            currentInputIndex++;
        }

        // 6. Inputs de SFX
        int sfxIdx = 0;
        for (TimelineEntry entry : timeline.entries()) {
            for (SfxCue sfx : entry.sfxCues()) {
                if (sfx.sfxPath() != null && new File(sfx.sfxPath()).exists()) {
                    command.add("-i");
                    command.add(sfx.sfxPath());

                    long sfxDelayMs = Math.round((entry.startTime() + sfx.timestampSeconds()) * 1000.0);
                    String sfxFilter = String.format(Locale.US,
                            "[%d:a]adelay=%d|%d,volume=%.2f[asfx_%d]",
                            currentInputIndex, sfxDelayMs, sfxDelayMs, sfx.volume(), sfxIdx);
                    audioFilterNodes.add(sfxFilter);
                    audioMixInputs.add(String.format("[asfx_%d]", sfxIdx));

                    sfxIdx++;
                    currentInputIndex++;
                }
            }
        }

        // 7. Mezcla de audio con normalización pedagógica EBU R128 (-14 LUFS)
        StringBuilder amixFilter = new StringBuilder();
        for (String inputTag : audioMixInputs) {
            amixFilter.append(inputTag);
        }
        amixFilter.append(String.format(
                "amix=inputs=%d:duration=first:dropout_transition=2:normalize=0[amixout];[amixout]loudnorm=I=-14:LRA=11:TP=-1.5[aout]",
                audioMixInputs.size()));
        audioFilterNodes.add(amixFilter.toString());

        // 8. Filter complex
        List<String> allFilters = new ArrayList<>();
        allFilters.addAll(videoFilterNodes);
        allFilters.addAll(audioFilterNodes);
        command.add("-filter_complex");
        command.add(String.join(";", allFilters));

        // 9. Mappings
        command.add("-map");
        command.add("[vout]");
        command.add("-map");
        command.add("[aout]");

        // 10. Codecs
        command.add("-c:v");
        command.add(chosenEncoder);
        if ("h264_nvenc".equalsIgnoreCase(chosenEncoder)) {
            command.add("-preset");
            command.add("p4");
            command.add("-cq");
            command.add(String.valueOf(defaultCrf));
        } else {
            command.add("-preset");
            command.add(defaultPreset);
            command.add("-crf");
            command.add(String.valueOf(defaultCrf));
        }
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-r");
        command.add(String.valueOf(fps));

        // Audio codec
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-ar");
        command.add("44100");

        command.add("-movflags");
        command.add("+faststart");

        command.add(outputPath.toAbsolutePath().toString());

        log.debug("Comando FFmpeg construido con {} argumentos y encoder {}", command.size(), chosenEncoder);
        return command;
    }

    private String resolveZoomExpression(CameraMotion motion, int frames) {
        if (motion == null) motion = CameraMotion.STATIC;

        return switch (motion) {
            case SLOW_ZOOM_IN -> "z='min(zoom+0.0006,1.20)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'";
            case SLOW_ZOOM_OUT -> "z='if(eq(on,1),1.20,max(zoom-0.0006,1.0))':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'";
            case PAN_LEFT -> String.format(Locale.US, "z=1.12:x='(1-on/%d)*iw*0.08':y='ih/2-(ih/zoom/2)'", frames);
            case PAN_RIGHT -> String.format(Locale.US, "z=1.12:x='(on/%d)*iw*0.08':y='ih/2-(ih/zoom/2)'", frames);
            case STATIC -> "z=1.0:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'";
        };
    }

    private String buildDuckingVolumeExpression(List<VolumeDuckPoint> duckingPoints, double baseVolume) {
        if (duckingPoints == null || duckingPoints.isEmpty()) {
            return String.format(Locale.US, "volume=%.2f", baseVolume);
        }

        List<String> betweenConditions = new ArrayList<>();
        for (VolumeDuckPoint point : duckingPoints) {
            betweenConditions.add(String.format(Locale.US, "between(t,%.2f,%.2f)", point.startTime(), point.endTime()));
        }

        String condition = String.join("+", betweenConditions);
        // Cuando hay locución hablada, atenuar al 0.05 (-26 dB), si no mantener baseVolume
        return String.format(Locale.US, "volume='if(%s,0.05,%.2f)':eval=frame", condition, baseVolume);
    }
}
