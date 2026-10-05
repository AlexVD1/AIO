package com.kidsanim.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kids.video")
public record KidsVideoProperties(
        String ffmpegPath,
        String ffprobePath,
        Integer width,
        Integer height,
        Integer fps,
        String encoder,
        String fallbackEncoder,
        String preset,
        Integer crf,
        Long renderTimeoutSeconds,
        String audioLibraryPath,
        Double bgmVolume,
        String exportPath,
        String fontName
) {
    public KidsVideoProperties {
        if (ffmpegPath == null || ffmpegPath.isBlank()) ffmpegPath = "ffmpeg";
        if (ffprobePath == null || ffprobePath.isBlank()) ffprobePath = "ffprobe";
        if (width == null || width <= 0) width = 1920;
        if (height == null || height <= 0) height = 1080;
        if (fps == null || fps <= 0) fps = 30;
        if (encoder == null || encoder.isBlank()) encoder = "h264_nvenc";
        if (fallbackEncoder == null || fallbackEncoder.isBlank()) fallbackEncoder = "libx264";
        if (preset == null || preset.isBlank()) preset = "medium";
        if (crf == null) crf = 22;
        if (renderTimeoutSeconds == null || renderTimeoutSeconds <= 0) renderTimeoutSeconds = 900L;
        if (audioLibraryPath == null || audioLibraryPath.isBlank()) audioLibraryPath = "./audio";
        if (bgmVolume == null) bgmVolume = 0.12;
        if (exportPath == null || exportPath.isBlank()) exportPath = "./export_videos";
        if (fontName == null || fontName.isBlank()) fontName = "Arial Rounded MT Bold, Arial, sans-serif";
    }

    public KidsVideoProperties() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
