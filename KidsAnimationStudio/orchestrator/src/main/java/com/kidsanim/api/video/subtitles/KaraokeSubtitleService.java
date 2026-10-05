package com.kidsanim.api.video.subtitles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.enums.AssetType;
import com.kidsanim.api.infrastructure.config.KidsVideoProperties;
import com.kidsanim.api.repository.AssetRepository;
import com.kidsanim.api.video.overlay.OverlayService;
import com.kidsanim.api.video.timeline.model.TimelineEntry;
import com.kidsanim.api.video.timeline.model.VideoTimeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class KaraokeSubtitleService {

    private static final Logger log = LoggerFactory.getLogger(KaraokeSubtitleService.class);

    private final AssetRepository assetRepository;
    private final OverlayService overlayService;
    private final KidsVideoProperties videoProperties;
    private final ObjectMapper objectMapper;

    public KaraokeSubtitleService(
            AssetRepository assetRepository,
            OverlayService overlayService,
            KidsVideoProperties videoProperties,
            ObjectMapper objectMapper) {
        this.assetRepository = assetRepository;
        this.overlayService = overlayService;
        this.videoProperties = videoProperties;
        this.objectMapper = objectMapper;
    }

    public Path writeKaraokeAssFile(VideoTimeline timeline, Path targetPath) {
        if (timeline == null) {
            throw new IllegalArgumentException("El timeline no puede ser nulo para generar subtítulos karaoke");
        }

        int width = timeline.width() > 0 ? timeline.width() : 1920;
        int height = timeline.height() > 0 ? timeline.height() : 1080;
        int fontSize = width > height ? 52 : 46;
        int marginV = width > height ? 80 : 160;

        StringBuilder sb = new StringBuilder();
        // 1. Script Info
        sb.append("[Script Info]\n");
        sb.append("Title: Kids Animation Studio Karaoke Subtitles\n");
        sb.append("ScriptType: v4.00+\n");
        sb.append("WrapStyle: 0\n");
        sb.append("ScaledBorderAndShadow: yes\n");
        sb.append(String.format("PlayResX: %d\n", width));
        sb.append(String.format("PlayResY: %d\n\n", height));

        // 2. V4+ Styles
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");

        String fontName = videoProperties != null && videoProperties.fontName() != null
                ? videoProperties.fontName()
                : "Arial Rounded MT Bold";

        // Estilo Karaoke infantil: Blanco con resalte Karaoke en amarillo brillante (&H0000FFFF), borde negro grueso
        sb.append(String.format(Locale.US,
                "Style: KidsKaraoke,%s,%d,&H00FFFFFF,&H0000FFFF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,4.5,2.0,2,60,60,%d,1\n",
                fontName, fontSize, marginV));

        // Estilos de Overlays pedagógicos
        List<String> overlayStyles = overlayService.generateOverlayStyles(width, height);
        for (String styleLine : overlayStyles) {
            sb.append(styleLine).append("\n");
        }
        sb.append("\n");

        // 3. Events
        sb.append("[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");

        for (TimelineEntry entry : timeline.entries()) {
            List<OverlayService.WordTiming> wordTimings = loadWordTimings(entry);

            // Generar eventos de diálogo de Karaoke
            List<String> karaokeLines = buildKaraokeDialogueLines(entry, wordTimings);
            for (String line : karaokeLines) {
                sb.append(line).append("\n");
            }

            // Generar eventos de Overlays educativos sincronizados
            List<String> overlayEvents = overlayService.generateOverlayEvents(entry, wordTimings, width, height);
            for (String overlayEvent : overlayEvents) {
                sb.append(overlayEvent).append("\n");
            }
        }

        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.writeString(targetPath, sb.toString(), StandardCharsets.UTF_8);
            log.info("Archivo ASS de subtítulos karaoke y overlays generado en: {}", targetPath.toAbsolutePath());
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Error al escribir el archivo ASS de subtítulos: " + targetPath, e);
        }
    }

    private List<OverlayService.WordTiming> loadWordTimings(TimelineEntry entry) {
        List<OverlayService.WordTiming> list = new ArrayList<>();
        var assetOpt = assetRepository.findByShotIdAndType(entry.shotId(), AssetType.WORD_TIMESTAMPS);
        if (assetOpt.isPresent() && assetOpt.get().getMetaJson() != null) {
            try {
                JsonNode root = objectMapper.readTree(assetOpt.get().getMetaJson());
                JsonNode wordsNode = root.path("words");
                if (wordsNode.isArray()) {
                    for (JsonNode w : wordsNode) {
                        String word = w.path("word").asText("");
                        double start = w.path("startMs").asDouble(0) / 1000.0;
                        double end = w.path("endMs").asDouble(0) / 1000.0;
                        if (!word.isBlank() && end >= start) {
                            list.add(new OverlayService.WordTiming(word.trim(), start, end));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudieron parsear timestamps de palabras para plano {}: {}", entry.shotId(), e.getMessage());
            }
        }
        return list;
    }

    private List<String> buildKaraokeDialogueLines(TimelineEntry entry, List<OverlayService.WordTiming> wordTimings) {
        List<String> lines = new ArrayList<>();
        String narration = entry.narrationText();
        if (narration == null || narration.isBlank()) {
            return lines;
        }

        double baseTime = entry.startTime() + entry.narrationStartOffset();

        if (wordTimings != null && !wordTimings.isEmpty()) {
            // Dividir palabras en bloques pequeños (máximo 4 palabras para preescolar)
            int chunkSize = 4;
            for (int i = 0; i < wordTimings.size(); i += chunkSize) {
                int endIdx = Math.min(i + chunkSize, wordTimings.size());
                List<OverlayService.WordTiming> chunk = wordTimings.subList(i, endIdx);

                double chunkStart = baseTime + chunk.get(0).startSeconds();
                double chunkEnd = baseTime + chunk.get(chunk.size() - 1).endSeconds() + 0.35;
                chunkEnd = Math.min(entry.endTime(), chunkEnd);

                StringBuilder karaokeText = new StringBuilder();
                for (int j = 0; j < chunk.size(); j++) {
                    OverlayService.WordTiming wt = chunk.get(j);
                    double durationSec = wt.endSeconds() - wt.startSeconds();
                    long durationCentis = Math.max(12, Math.round(durationSec * 100.0));

                    // Si hay un espacio/pausa antes de la siguiente palabra, agregarla al centis
                    if (j < chunk.size() - 1) {
                        double nextStart = chunk.get(j + 1).startSeconds();
                        if (nextStart > wt.endSeconds()) {
                            durationCentis += Math.round((nextStart - wt.endSeconds()) * 100.0);
                        }
                    }

                    karaokeText.append(String.format("{\\k%d}%s ", durationCentis, sanitizeAssText(wt.word())));
                }

                String startAss = formatAssTime(chunkStart);
                String endAss = formatAssTime(chunkEnd);
                lines.add(String.format(Locale.US, "Dialogue: 0,%s,%s,KidsKaraoke,,0,0,0,,%s",
                        startAss, endAss, karaokeText.toString().trim()));
            }
        } else {
            // Fallback si no hay timestamps por palabra: mostrar texto completo con fade
            double start = baseTime;
            double end = Math.min(entry.endTime(), start + Math.max(2.0, entry.narrationDuration()));
            String startAss = formatAssTime(start);
            String endAss = formatAssTime(end);
            lines.add(String.format(Locale.US, "Dialogue: 0,%s,%s,KidsKaraoke,,0,0,0,,%s",
                    startAss, endAss, sanitizeAssText(narration)));
        }

        return lines;
    }

    private String sanitizeAssText(String text) {
        if (text == null) return "";
        return text.replace("\\", "")
                .replace("{", "")
                .replace("}", "");
    }

    private String formatAssTime(double seconds) {
        long totalCentis = Math.max(0, Math.round(seconds * 100.0));
        long hrs = totalCentis / 360000;
        long mins = (totalCentis % 360000) / 6000;
        long secs = (totalCentis % 6000) / 100;
        long centis = totalCentis % 100;
        return String.format(Locale.US, "%d:%02d:%02d.%02d", hrs, mins, secs, centis);
    }
}
