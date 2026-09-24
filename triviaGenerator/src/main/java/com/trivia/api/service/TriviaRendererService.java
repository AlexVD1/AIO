package com.trivia.api.service;

import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaOpcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio encargado de renderizar imágenes PNG de alta resolución (1080x1080)
 * para cada trivia utilizando la API estándar Java AWT y Graphics2D.
 *
 * MODOS DE RENDERIZADO:
 *   1. PREGUNTA (TipoAsset.PREGUNTA): Tarjeta con tema, dificultad, pregunta y opciones
 *      sin revelar la respuesta correcta.
 *   2. RESPUESTA (TipoAsset.RESPUESTA): Idéntica composición visual, pero destacando
 *      la opción correcta con color verde esmeralda y mostrando la tarjeta de explicación.
 */
@Service
public class TriviaRendererService {

    private static final Logger log = LoggerFactory.getLogger(TriviaRendererService.class);

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1080;

    // Colores de la paleta moderna (Dark Indigo / Slate)
    private static final Color BG_TOP = new Color(15, 23, 42);      // #0f172a
    private static final Color BG_BOTTOM = new Color(30, 41, 59);   // #1e293b
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);

    private static final Color BADGE_CATEGORY_BG = new Color(37, 99, 235);  // Blue 600
    private static final Color BADGE_DIFFICULTY_BG = new Color(79, 70, 229); // Indigo 600

    private static final Color OPTION_DEFAULT_BG = new Color(51, 65, 85);    // Slate 700
    private static final Color OPTION_DEFAULT_BORDER = new Color(71, 85, 105);
    private static final Color OPTION_LETTER_BG = new Color(30, 41, 59);

    private static final Color OPTION_CORRECT_BG = new Color(16, 185, 129);  // Emerald 500
    private static final Color OPTION_CORRECT_LETTER = new Color(4, 120, 87);
    private static final Color OPTION_DIMMED_BG = new Color(30, 41, 59, 160);

    private static final Color EXPLANATION_BG = new Color(30, 41, 59, 220);
    private static final Color EXPLANATION_ACCENT = new Color(251, 191, 36); // Amber 400

    public byte[] renderPregunta(Trivia trivia, List<TriviaOpcion> opciones) {
        return render(trivia, opciones, false);
    }

    public byte[] renderRespuesta(Trivia trivia, List<TriviaOpcion> opciones) {
        return render(trivia, opciones, true);
    }

    private byte[] render(Trivia trivia, List<TriviaOpcion> opciones, boolean esModoRespuesta) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // Activar renderizado de alta calidad y anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 1. Fondo degradado
            GradientPaint gradient = new GradientPaint(0, 0, BG_TOP, 0, HEIGHT, BG_BOTTOM);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            int cursorY = 70;

            // 2. Badges de categoría y dificultad en el encabezado
            String categoria = trivia.getTipoTrivia() != null ? trivia.getTipoTrivia().getNombre() : "TRIVIA";
            String dificultad = "DIFICULTAD: " + (trivia.getDificultad() != null ? trivia.getDificultad().name() : "MEDIA");

            cursorY = drawHeaderBadges(g2d, categoria, dificultad, cursorY);

            // 3. Texto de la pregunta
            cursorY += 40;
            cursorY = drawQuestionText(g2d, trivia.getPregunta(), cursorY);

            // 4. Opciones de respuesta
            cursorY += 35;
            cursorY = drawOptions(g2d, opciones, esModoRespuesta, cursorY);

            // 5. Explicación (solo en modo respuesta)
            if (esModoRespuesta && trivia.getExplicacion() != null && !trivia.getExplicacion().isBlank()) {
                drawExplanation(g2d, trivia.getExplicacion(), cursorY + 25);
            } else if (!esModoRespuesta) {
                drawFooter(g2d, trivia.getSubtema());
            }

            // Codificar a PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error al renderizar imagen PNG de trivia", e);
            throw new IllegalStateException("Error al renderizar imagen: " + e.getMessage(), e);
        } finally {
            g2d.dispose();
        }
    }

    private int drawHeaderBadges(Graphics2D g, String categoria, String dificultad, int y) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();

        // Badge 1: Categoría
        int padH = 24;
        int padV = 10;
        int catWidth = fm.stringWidth(categoria.toUpperCase()) + (padH * 2);
        int badgeHeight = fm.getHeight() + (padV * 2);

        int startX = 60;
        g.setColor(BADGE_CATEGORY_BG);
        g.fill(new RoundRectangle2D.Float(startX, y, catWidth, badgeHeight, 20, 20));
        g.setColor(TEXT_WHITE);
        g.drawString(categoria.toUpperCase(), startX + padH, y + padV + fm.getAscent());

        // Badge 2: Dificultad
        int difWidth = fm.stringWidth(dificultad) + (padH * 2);
        int difX = startX + catWidth + 18;
        g.setColor(BADGE_DIFFICULTY_BG);
        g.fill(new RoundRectangle2D.Float(difX, y, difWidth, badgeHeight, 20, 20));
        g.setColor(TEXT_WHITE);
        g.drawString(dificultad, difX + padH, y + padV + fm.getAscent());

        return y + badgeHeight;
    }

    private int drawQuestionText(Graphics2D g, String pregunta, int y) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        g.setColor(TEXT_WHITE);
        FontMetrics fm = g.getFontMetrics();

        int maxWidth = WIDTH - 120; // 60px margin left/right
        List<String> lineas = wrapText(pregunta, fm, maxWidth);

        int currentY = y;
        for (String linea : lineas) {
            g.drawString(linea, 60, currentY + fm.getAscent());
            currentY += fm.getHeight() + 8;
        }

        return currentY;
    }

    private int drawOptions(Graphics2D g, List<TriviaOpcion> opciones, boolean esModoRespuesta, int y) {
        if (opciones == null || opciones.isEmpty()) {
            return y;
        }

        int currentY = y;
        int cardWidth = WIDTH - 120;
        int cardHeight = 70;
        int gap = 16;

        for (TriviaOpcion opc : opciones) {
            boolean esCorrecta = Boolean.TRUE.equals(opc.getCorrecta());

            Color bg;
            Color border;
            Color letterBg;
            Color textColor;

            if (esModoRespuesta) {
                if (esCorrecta) {
                    bg = OPTION_CORRECT_BG;
                    border = new Color(52, 211, 153);
                    letterBg = OPTION_CORRECT_LETTER;
                    textColor = TEXT_WHITE;
                } else {
                    bg = OPTION_DIMMED_BG;
                    border = new Color(51, 65, 85, 100);
                    letterBg = new Color(15, 23, 42, 120);
                    textColor = TEXT_MUTED;
                }
            } else {
                bg = OPTION_DEFAULT_BG;
                border = OPTION_DEFAULT_BORDER;
                letterBg = OPTION_LETTER_BG;
                textColor = TEXT_WHITE;
            }

            // Rectángulo redondeado de la opción
            RoundRectangle2D.Float card = new RoundRectangle2D.Float(60, currentY, cardWidth, cardHeight, 18, 18);
            g.setColor(bg);
            g.fill(card);
            g.setColor(border);
            g.setStroke(new BasicStroke(2.0f));
            g.draw(card);

            // Cuadro de la letra
            int letterBoxSize = cardHeight - 16;
            RoundRectangle2D.Float letterBox = new RoundRectangle2D.Float(68, currentY + 8, letterBoxSize, letterBoxSize, 14, 14);
            g.setColor(letterBg);
            g.fill(letterBox);

            // Letra
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            FontMetrics fmLetter = g.getFontMetrics();
            int letterX = 68 + (letterBoxSize - fmLetter.stringWidth(opc.getLetra())) / 2;
            int letterY = currentY + 8 + (letterBoxSize - fmLetter.getHeight()) / 2 + fmLetter.getAscent();
            g.setColor(TEXT_WHITE);
            g.drawString(opc.getLetra(), letterX, letterY);

            // Texto de la opción
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
            FontMetrics fmText = g.getFontMetrics();
            g.setColor(textColor);

            String texto = opc.getTexto();
            if (esModoRespuesta && esCorrecta) {
                texto = texto + "  ✓";
            }

            // Truncar con ellipsis si es muy largo para una sola línea
            int maxTextWidth = cardWidth - letterBoxSize - 50;
            if (fmText.stringWidth(texto) > maxTextWidth) {
                while (texto.length() > 3 && fmText.stringWidth(texto + "...") > maxTextWidth) {
                    texto = texto.substring(0, texto.length() - 1);
                }
                texto = texto + "...";
            }

            int textY = currentY + (cardHeight - fmText.getHeight()) / 2 + fmText.getAscent();
            g.drawString(texto, 60 + letterBoxSize + 28, textY);

            currentY += cardHeight + gap;
        }

        return currentY;
    }

    private void drawExplanation(Graphics2D g, String explicacion, int y) {
        int cardWidth = WIDTH - 120;
        int cardHeight = HEIGHT - y - 60;
        if (cardHeight < 100) return;

        RoundRectangle2D.Float expCard = new RoundRectangle2D.Float(60, y, cardWidth, cardHeight, 18, 18);
        g.setColor(EXPLANATION_BG);
        g.fill(expCard);
        g.setColor(new Color(251, 191, 36, 120));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(expCard);

        int currentY = y + 26;

        // Título de la explicación
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        FontMetrics fmTitle = g.getFontMetrics();
        g.setColor(EXPLANATION_ACCENT);
        g.drawString("💡 EXPLICACIÓN", 85, currentY + fmTitle.getAscent());
        currentY += fmTitle.getHeight() + 10;

        // Texto de la explicación
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        FontMetrics fmExp = g.getFontMetrics();
        g.setColor(new Color(226, 232, 240));

        List<String> lineas = wrapText(explicacion, fmExp, cardWidth - 50);
        for (String linea : lineas) {
            if (currentY + fmExp.getHeight() > y + cardHeight - 15) {
                break;
            }
            g.drawString(linea, 85, currentY + fmExp.getAscent());
            currentY += fmExp.getHeight() + 4;
        }
    }

    private void drawFooter(Graphics2D g, String subtema) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(TEXT_MUTED);
        FontMetrics fm = g.getFontMetrics();

        String footerText = (subtema != null && !subtema.isBlank())
                ? "Subtema: " + subtema
                : "Plataforma de Trivias con IA";

        int textX = (WIDTH - fm.stringWidth(footerText)) / 2;
        g.drawString(footerText, textX, HEIGHT - 50);
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
