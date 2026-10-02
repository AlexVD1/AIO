package com.storyvideo.api.export.dto;

import java.util.List;
import java.util.UUID;

public record ExportMetadata(
        UUID storyId,
        String title,
        String description,
        List<String> hashtags,
        String genre,
        String category,
        String language,
        double duration_seconds,
        String resolution,
        String format,
        String codec_video,
        String codec_audio,
        int fps,
        String created_at,
        String video_file,
        int scene_count,
        boolean has_subtitles,
        boolean has_narration,
        boolean has_music,
        String tts_voice
) {}
