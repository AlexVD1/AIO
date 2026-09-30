package com.trivia.api.service.video;

/**
 * Formatos de aspecto y resolución soportados para la generación de videos de trivias.
 */
public enum VideoFormat {

    /**
     * Formato vertical 9:16 (1080x1920) optimizado para TikTok, Instagram Reels y YouTube Shorts.
     */
    VERTICAL_9_16(1080, 1920),

    /**
     * Formato cuadrado 1:1 (1080x1080) idéntico a las dimensiones de las tarjetas de trivia generadas.
     */
    SQUARE_1_1(1080, 1080),

    /**
     * Formato panorámico 16:9 (1920x1080) para videos estándar de YouTube o pantallas de escritorio.
     */
    HORIZONTAL_16_9(1920, 1080);

    private final int width;
    private final int height;

    VideoFormat(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
