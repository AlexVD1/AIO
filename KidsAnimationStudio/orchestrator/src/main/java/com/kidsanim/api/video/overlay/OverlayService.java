package com.kidsanim.api.video.overlay;

import com.kidsanim.api.script.model.OverlaySpec;
import com.kidsanim.api.video.timeline.model.TimelineEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OverlayService {

    private static final Logger log = LoggerFactory.getLogger(OverlayService.class);

    public record WordTiming(String word, double startSeconds, double endSeconds) {}

    public List<String> generateOverlayStyles(int width, int height) {
        int bigFontSize = width > height ? 130 : 110;
        int badgeFontSize = width > height ? 90 : 75;

        List<String> styles = new ArrayList<>();
        // Style: BigNumber / Letter: Centered anchor (Alignment 5), thick outline, shadow
        styles.add(String.format(Locale.US,
                "Style: KidsOverlayBig,Arial Rounded MT Bold,%d,&H0000FFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,6.0,3.0,5,30,30,30,1",
                bigFontSize));

        // Style: Shape / Counter / Color swatch
        styles.add(String.format(Locale.US,
                "Style: KidsOverlayBadge,Arial Rounded MT Bold,%d,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,5.0,2.5,5,30,30,30,1",
                badgeFontSize));

        return styles;
    }

    public List<String> generateOverlayEvents(TimelineEntry entry, List<WordTiming> words, int width, int height) {
        if (entry.overlays() == null || entry.overlays().isEmpty()) {
            return List.of();
        }

        List<String> events = new ArrayList<>();

        for (OverlaySpec overlay : entry.overlays()) {
            if (overlay.value() == null || overlay.value().isBlank()) {
                continue;
            }

            // 1. Determinar el timestamp de inicio sincronizado con appearAtWord
            double overlayStart = entry.startTime() + entry.narrationStartOffset();
            if (overlay.appearAtWord() != null && !overlay.appearAtWord().isBlank() && words != null) {
                String targetWord = normalize(overlay.appearAtWord());
                for (WordTiming wt : words) {
                    if (normalize(wt.word()).contains(targetWord) || targetWord.contains(normalize(wt.word()))) {
                        overlayStart = entry.startTime() + entry.narrationStartOffset() + wt.startSeconds();
                        break;
                    }
                }
            }

            double overlayEnd = entry.endTime();
            if (overlayStart >= overlayEnd) {
                overlayStart = Math.max(entry.startTime(), overlayEnd - 1.5);
            }

            String startAss = formatAssTime(overlayStart);
            String endAss = formatAssTime(overlayEnd);

            // 2. Determinar coordenadas según la posición y resolución
            int posX = resolveX(overlay.position(), width);
            int posY = resolveY(overlay.position(), height);

            // 3. Estilo y formateo de contenido con animación pop-in didáctica
            String type = overlay.type() != null ? overlay.type().toUpperCase() : "BIG_NUMBER";
            String styleName = "KidsOverlayBig";
            String formattedText;

            // Animación de pop-in: escala 130% en 180 ms y regresa a 100% en 120 ms con fade
            String popInAnimation = String.format(Locale.US, "{\\pos(%d,%d)\\fad(150,200)\\t(0,180,\\fscx130\\fscy130)\\t(180,300,\\fscx100\\fscy100)}", posX, posY);

            switch (type) {
                case "BIG_NUMBER" -> {
                    styleName = "KidsOverlayBig";
                    formattedText = popInAnimation + "{\\c&H00D7FF&}" + overlay.value(); // Amarillo dorado
                }
                case "LETTER" -> {
                    styleName = "KidsOverlayBig";
                    formattedText = popInAnimation + "{\\c&HFF69B4&}" + overlay.value().toUpperCase(); // Rosa didáctico
                }
                case "COUNTER" -> {
                    styleName = "KidsOverlayBadge";
                    formattedText = popInAnimation + "{\\c&H00FF7F&}⭐ x " + overlay.value(); // Verde menta
                }
                case "SHAPE" -> {
                    styleName = "KidsOverlayBadge";
                    String shapeGlyph = resolveShapeGlyph(overlay.value());
                    formattedText = popInAnimation + "{\\c&H00FFFF&}" + shapeGlyph + " " + overlay.value().toUpperCase();
                }
                case "COLOR_SWATCH" -> {
                    styleName = "KidsOverlayBadge";
                    String colorBgr = resolveColorBgr(overlay.value());
                    formattedText = popInAnimation + "{\\c" + colorBgr + "&}● " + overlay.value().toUpperCase();
                }
                default -> {
                    styleName = "KidsOverlayBadge";
                    formattedText = popInAnimation + overlay.value();
                }
            }

            String eventLine = String.format(Locale.US, "Dialogue: 1,%s,%s,%s,,0,0,0,,%s",
                    startAss, endAss, styleName, formattedText);
            events.add(eventLine);
        }

        return events;
    }

    private int resolveX(String position, int width) {
        if (position == null) position = "TOP_RIGHT";
        return switch (position.toUpperCase()) {
            case "TOP_LEFT", "BOTTOM_LEFT", "LEFT" -> (int) (width * 0.18);
            case "CENTER", "TOP_CENTER", "BOTTOM_CENTER" -> (int) (width * 0.50);
            case "TOP_RIGHT", "BOTTOM_RIGHT", "RIGHT" -> (int) (width * 0.82);
            default -> (int) (width * 0.82);
        };
    }

    private int resolveY(String position, int height) {
        if (position == null) position = "TOP_RIGHT";
        return switch (position.toUpperCase()) {
            case "TOP_LEFT", "TOP_RIGHT", "TOP_CENTER" -> (int) (height * 0.22);
            case "CENTER", "LEFT", "RIGHT" -> (int) (height * 0.50);
            case "BOTTOM_LEFT", "BOTTOM_RIGHT", "BOTTOM_CENTER" -> (int) (height * 0.78);
            default -> (int) (height * 0.22);
        };
    }

    private String resolveShapeGlyph(String shape) {
        if (shape == null) return "●";
        String s = shape.toUpperCase();
        if (s.contains("CIRC") || s.contains("CÍRC")) return "●";
        if (s.contains("CUADR") || s.contains("SQUARE")) return "■";
        if (s.contains("TRIAN") || s.contains("TRIÁN")) return "▲";
        if (s.contains("ESTR") || s.contains("STAR")) return "★";
        if (s.contains("CORAZ") || s.contains("HEART")) return "♥";
        return "◆";
    }

    private String resolveColorBgr(String colorName) {
        if (colorName == null) return "&H00FFFF";
        String c = colorName.toLowerCase();
        if (c.contains("rojo") || c.contains("red")) return "&H0000FF";
        if (c.contains("azul") || c.contains("blue")) return "&HFF6000";
        if (c.contains("amarillo") || c.contains("yellow")) return "&H00FFFF";
        if (c.contains("verde") || c.contains("green")) return "&H00FF00";
        if (c.contains("naranja") || c.contains("orange")) return "&H0080FF";
        if (c.contains("morado") || c.contains("purple") || c.contains("violeta")) return "&HFF0080";
        if (c.contains("rosa") || c.contains("pink")) return "&HCB60FF";
        return "&H00FFFF";
    }

    private String normalize(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text.toLowerCase().trim(), Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}\\p{Punct}]", "");
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
