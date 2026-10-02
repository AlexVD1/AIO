package com.storyvideo.api.video.ffmpeg;

import com.storyvideo.api.domain.CameraMovement;
import com.storyvideo.api.domain.TransitionType;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.video.editing.service.DynamicEditingService;
import com.storyvideo.api.video.timeline.model.SfxCue;
import com.storyvideo.api.video.timeline.model.TimelineEntry;
import com.storyvideo.api.video.timeline.model.VideoTimeline;
import com.storyvideo.api.video.timeline.model.VolumeDuckPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FFmpegCommandBuilder {

    private static final Logger log = LoggerFactory.getLogger(FFmpegCommandBuilder.class);

    private final String ffmpegPath;
    private final int width;
    private final int height;
    private final int fps;
    private final int crf;
    private final String preset;
    private final DynamicEditingService dynamicEditingService;

    @org.springframework.beans.factory.annotation.Autowired
    public FFmpegCommandBuilder(
            @Value("${story.video.ffmpeg-path:ffmpeg}") String ffmpegPath,
            @Value("${story.video.width:1080}") int width,
            @Value("${story.video.height:1920}") int height,
            @Value("${story.video.fps:30}") int fps,
            @Value("${story.video.crf:22}") int crf,
            @Value("${story.video.preset:medium}") String preset,
            com.storyvideo.api.video.editing.service.DynamicEditingService dynamicEditingService) {
        this.ffmpegPath = ffmpegPath;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.crf = crf;
        this.preset = preset;
        this.dynamicEditingService = dynamicEditingService != null ? dynamicEditingService : new com.storyvideo.api.video.editing.service.DynamicEditingService();
    }

    public FFmpegCommandBuilder(String ffmpegPath, int width, int height, int fps, int crf, String preset) {
        this(ffmpegPath, width, height, fps, crf, preset, new com.storyvideo.api.video.editing.service.DynamicEditingService());
    }

    public List<String> buildRenderCommand(VideoTimeline timeline, Path outputPath) {
        return buildRenderCommand(timeline, outputPath, null, null);
    }

    public List<String> buildRenderCommand(VideoTimeline timeline, Path outputPath, Path subtitleSrtPath, com.storyvideo.api.domain.StoryGenre genre) {
        if (timeline == null || timeline.entries() == null || timeline.entries().isEmpty()) {
            throw new IllegalArgumentException("No se puede generar comando FFmpeg para un timeline vacío");
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-hide_banner");
        command.add("-y"); // Sobrescribir salida

        List<String> videoFilterNodes = new ArrayList<>();
        List<String> audioFilterNodes = new ArrayList<>();
        List<String> audioMixInputs = new ArrayList<>();

        int currentInputIndex = 0;

        // 1. Inputs de video (imágenes por escena)
        for (int i = 0; i < timeline.entries().size(); i++) {
            TimelineEntry entry = timeline.entries().get(i);
            double duration = entry.duration();
            int frames = (int) Math.max(1, Math.round(duration * fps));

            boolean hasImageFile = entry.imagePath() != null && new File(entry.imagePath()).exists();

            if (hasImageFile) {
                command.add("-loop");
                command.add("1");
                command.add("-t");
                command.add(String.format(Locale.US, "%.2f", duration));
                command.add("-i");
                command.add(entry.imagePath());
            } else {
                // Fallback lavfi si no hay imagen física disponible
                command.add("-f");
                command.add("lavfi");
                command.add("-t");
                command.add(String.format(Locale.US, "%.2f", duration));
                command.add("-i");
                command.add(String.format("color=c=0x111118:s=%dx%d:r=%d", width, height, fps));
            }

            // Construir filtergraph para este clip de video
            String zoomExpr = resolveZoomExpression(entry.cameraMovement(), frames);
            String transitionFilter = resolveTransitionFilter(entry.transitionIn(), entry.transitionOut(), duration);
            String visualEffects = dynamicEditingService.resolveVisualEffectsFilter(genre, entry.visualEffects());

            String videoFilterNode = String.format(Locale.US,
                    "[%d:v]scale=2160:3840,zoompan=%s:d=%d:s=%dx%d:fps=%d,trim=duration=%.2f,setpts=PTS-STARTPTS%s%s[v%d]",
                    currentInputIndex, zoomExpr, frames, width, height, fps, duration, transitionFilter, visualEffects, i);

            videoFilterNodes.add(videoFilterNode);
            currentInputIndex++;
        }

        // 2. Concatenación de clips de video
        StringBuilder concatFilter = new StringBuilder();
        for (int i = 0; i < timeline.entries().size(); i++) {
            concatFilter.append(String.format("[v%d]", i));
        }

        boolean hasSubtitles = subtitleSrtPath != null && java.nio.file.Files.exists(subtitleSrtPath);
        String voutTarget = hasSubtitles ? "[vout_raw]" : "[vout]";
        concatFilter.append(String.format("concat=n=%d:v=1:a=0%s", timeline.entries().size(), voutTarget));
        videoFilterNodes.add(concatFilter.toString());

        // Overlay de subtítulos si el archivo de subtítulos está presente (.ass o .srt)
        if (hasSubtitles) {
            String safeSubPath = subtitleSrtPath.toAbsolutePath().toString().replace("\\", "/").replace(":", "\\:");
            String subtitleFilter;
            if (subtitleSrtPath.toString().toLowerCase().endsWith(".ass")) {
                subtitleFilter = String.format(Locale.US, "[vout_raw]ass='%s'[vout]", safeSubPath);
            } else {
                subtitleFilter = String.format(Locale.US,
                        "[vout_raw]subtitles='%s':force_style='PlayResX=1080,PlayResY=1920,FontName=Arial,FontSize=50,Bold=1,PrimaryColour=&H0000FFFF,OutlineColour=&H00000000,BorderStyle=1,Outline=3.5,Shadow=2,Alignment=2,MarginV=280'[vout]",
                        safeSubPath);
            }
            videoFilterNodes.add(subtitleFilter);
        }

        // 3. Audio Base: pista de silencio constante para prevenir recortes
        double totalDuration = timeline.totalDurationSeconds();
        audioFilterNodes.add(String.format(Locale.US, "aevalsrc=0:d=%.2f:s=44100:c=stereo[asilence]", totalDuration));
        audioMixInputs.add("[asilence]");

        // 4. Inputs de Narración
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

        // 5. Input de Música de Fondo (BGM)
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

        // 6. Inputs de SFX (Sound Effects)
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

        // 7. Mezcla de Audio amix
        StringBuilder amixFilter = new StringBuilder();
        for (String inputTag : audioMixInputs) {
            amixFilter.append(inputTag);
        }
        amixFilter.append(String.format(
                "amix=inputs=%d:duration=first:dropout_transition=2:normalize=0[amixout];[amixout]alimiter=limit=0.95[aout]",
                audioMixInputs.size()));
        audioFilterNodes.add(amixFilter.toString());

        // 8. Combinar filtros en -filter_complex
        List<String> allFilters = new ArrayList<>();
        allFilters.addAll(videoFilterNodes);
        allFilters.addAll(audioFilterNodes);
        String filterComplex = String.join(";", allFilters);

        command.add("-filter_complex");
        command.add(filterComplex);

        // 9. Mappings y Codecs
        command.add("-map");
        command.add("[vout]");
        command.add("-map");
        command.add("[aout]");

        // Codecs de video y audio
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(preset);
        command.add("-crf");
        command.add(String.valueOf(crf));
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-r");
        command.add(String.valueOf(fps));

        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-ar");
        command.add("44100");

        command.add("-movflags");
        command.add("+faststart");

        command.add(outputPath.toAbsolutePath().toString());

        log.debug("Comando FFmpeg generado con {} argumentos para salida {}", command.size(), outputPath);
        return command;
    }

    private String resolveZoomExpression(CameraMovement movement, int frames) {
        if (movement == null) movement = CameraMovement.SLOW_ZOOM_IN;

        return switch (movement) {
            case SLOW_ZOOM_IN -> "z='min(zoom+0.0008,1.25)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'";
            case SLOW_ZOOM_OUT -> "z='if(eq(on,1),1.25,max(zoom-0.0008,1.0))':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'";
            case PAN_LEFT -> String.format(Locale.US, "z=1.15:x='(1-on/%d)*iw*0.1':y='ih/2-(ih/zoom/2)'", frames);
            case PAN_RIGHT -> String.format(Locale.US, "z=1.15:x='(on/%d)*iw*0.1':y='ih/2-(ih/zoom/2)'", frames);
            case KEN_BURNS -> String.format(Locale.US, "z='min(zoom+0.0008,1.20)':x='(on/%d)*iw*0.08':y='(on/%d)*ih*0.08'", frames, frames);
            case PARALLAX -> String.format(Locale.US, "z='min(zoom+0.001,1.20)':x='iw/2-(iw/zoom/2)':y='(on/%d)*ih*0.10'", frames);
            case STATIC -> "z=1.0:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'";
        };
    }

    private String resolveTransitionFilter(TransitionType in, TransitionType out, double duration) {
        StringBuilder sb = new StringBuilder();
        if (in == TransitionType.FADE_IN) {
            sb.append(",fade=t=in:st=0:d=0.35");
        } else if (in == TransitionType.FLASH) {
            sb.append(",fade=t=in:st=0:d=0.20:color=white");
        }

        if (out == TransitionType.FADE_OUT) {
            double startTime = Math.max(0.0, duration - 0.35);
            sb.append(String.format(Locale.US, ",fade=t=out:st=%.2f:d=0.35", startTime));
        }

        return sb.toString();
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
        // Cuando alguna condición between es 1, baja al 0.05, sino baseVolume
        return String.format(Locale.US, "volume='if(%s,0.05,%.2f)':eval=frame", condition, baseVolume);
    }
}
