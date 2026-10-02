package com.storyvideo.api.image;

import com.storyvideo.api.image.dto.GeneratedImage;
import com.storyvideo.api.image.dto.ImageGenerationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service("placeholderImageService")
public class PlaceholderImageService implements ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PlaceholderImageService.class);

    @Override
    public String getProviderName() {
        return "Placeholder (AWT Headless Generator)";
    }

    @Override
    public int getEstimatedTimeSeconds() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public GeneratedImage generate(ImageGenerationRequest request) {
        long start = System.currentTimeMillis();
        int width = request.resolvedWidth();
        int height = request.resolvedHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Fondo oscuro cinematográfico con gradiente
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(15, 23, 42),
                    0, height, new Color(5, 8, 16)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);

            // Marco decorativo
            g2d.setColor(new Color(56, 189, 248, 40));
            g2d.setStroke(new BasicStroke(4.0f));
            g2d.drawRect(30, 30, width - 60, height - 60);

            // Título de Escena
            g2d.setColor(new Color(56, 189, 248));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 42));
            String header = "ESCENA " + request.sequenceNumber();
            FontMetrics fmHeader = g2d.getFontMetrics();
            int xHeader = (width - fmHeader.stringWidth(header)) / 2;
            g2d.drawString(header, xHeader, 200);

            // Badge de placeholder
            g2d.setColor(new Color(148, 163, 184));
            g2d.setFont(new Font("SansSerif", Font.ITALIC, 24));
            String badge = "[STABLE DIFFUSION PLACEHOLDER]";
            FontMetrics fmBadge = g2d.getFontMetrics();
            int xBadge = (width - fmBadge.stringWidth(badge)) / 2;
            g2d.drawString(badge, xBadge, 260);

            // Visual Prompt
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 28));
            String promptText = request.prompt() != null ? request.prompt() : "No visual prompt specified";

            // Envoltura de texto básica
            drawWrappedText(g2d, promptText, 80, 400, width - 160);

            // Información técnica en la parte inferior
            g2d.setColor(new Color(100, 116, 139));
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 20));
            g2d.drawString("Seed: " + request.resolvedSeed(), 80, height - 120);
            g2d.drawString("Resolution: " + width + "x" + height, 80, height - 90);

        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException("Error escribiendo imagen placeholder PNG", e);
        }

        byte[] bytes = baos.toByteArray();
        long duration = System.currentTimeMillis() - start;
        log.info("Placeholder generado para escena {} en {} ms", request.sequenceNumber(), duration);
        return GeneratedImage.png(bytes, request.resolvedSeed(), width, height, duration);
    }

    private void drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        int lineY = y;

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (fm.stringWidth(testLine) > maxWidth) {
                g2d.drawString(currentLine.toString(), x, lineY);
                lineY += fm.getHeight() + 8;
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (!currentLine.isEmpty()) {
            g2d.drawString(currentLine.toString(), x, lineY);
        }
    }
}
