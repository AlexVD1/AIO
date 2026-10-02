package com.storyvideo.api.video.subtitles.service;

import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryScene;
import com.storyvideo.api.video.timeline.model.TimelineEntry;
import com.storyvideo.api.video.timeline.model.VideoTimeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class SubtitleService {

    private static final Logger log = LoggerFactory.getLogger(SubtitleService.class);

    public record SubtitleEntry(int index, double startSeconds, double endSeconds, String text) {}

    public List<SubtitleEntry> buildSubtitleEntries(Story story, VideoTimeline timeline) {
        if (story == null || story.getScenes() == null || timeline == null || timeline.entries() == null) {
            return List.of();
        }

        Map<Integer, StoryScene> sceneMap = new HashMap<>();
        for (StoryScene s : story.getScenes()) {
            sceneMap.put(s.getSequenceNumber(), s);
        }

        List<SubtitleEntry> allEntries = new ArrayList<>();
        int globalIndex = 1;

        for (TimelineEntry entry : timeline.entries()) {
            StoryScene scene = sceneMap.get(entry.sequenceNumber());
            if (scene == null || scene.getNarrationText() == null || scene.getNarrationText().isBlank()) {
                continue;
            }

            double narrationStart = entry.startTime() + entry.narrationStartOffset();
            double narrationDuration = entry.narrationDuration() > 0 ? entry.narrationDuration() : entry.duration();

            List<String> chunks = splitTextIntoChunks(scene.getNarrationText(), 4);
            if (chunks.isEmpty()) continue;

            int totalChars = chunks.stream().mapToInt(String::length).sum();
            double currentCueStart = narrationStart;

            for (String chunk : chunks) {
                double chunkRatio = totalChars > 0 ? (double) chunk.length() / totalChars : 1.0 / chunks.size();
                double chunkDuration = Math.max(1.0, narrationDuration * chunkRatio);
                double currentCueEnd = Math.min(entry.endTime(), currentCueStart + chunkDuration);

                allEntries.add(new SubtitleEntry(globalIndex++, currentCueStart, currentCueEnd, chunk));
                currentCueStart = currentCueEnd;
            }
        }

        return allEntries;
    }

    public String generateSrtContent(List<SubtitleEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (SubtitleEntry entry : entries) {
            sb.append(entry.index()).append("\n");
            sb.append(formatSrtTime(entry.startSeconds())).append(" --> ").append(formatSrtTime(entry.endSeconds())).append("\n");
            sb.append(entry.text()).append("\n\n");
        }
        return sb.toString();
    }

    public Path writeSrtFile(Story story, VideoTimeline timeline, Path targetPath) {
        List<SubtitleEntry> entries = buildSubtitleEntries(story, timeline);
        String srtContent = generateSrtContent(entries);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, srtContent, StandardCharsets.UTF_8);
            log.info("Archivo SRT generado con {} subtítulos en: {}", entries.size(), targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Error al escribir archivo SRT de subtítulos: " + targetPath, e);
        }
    }

    public String generateAssContent(List<SubtitleEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Script Info]\n");
        sb.append("Title: Story Video Subtitles\n");
        sb.append("ScriptType: v4.00+\n");
        sb.append("WrapStyle: 0\n");
        sb.append("ScaledBorderAndShadow: yes\n");
        sb.append("PlayResX: 1080\n");
        sb.append("PlayResY: 1920\n\n");

        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        sb.append("Style: Default,Arial,50,&H0000FFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,3.5,2.0,2,80,80,280,1\n\n");

        sb.append("[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
        for (SubtitleEntry entry : entries) {
            String start = formatAssTime(entry.startSeconds());
            String end = formatAssTime(entry.endSeconds());
            String safeText = entry.text().replace("\\", "").replace("{", "").replace("}", "");
            sb.append(String.format("Dialogue: 0,%s,%s,Default,,0,0,0,,%s\n", start, end, safeText));
        }
        return sb.toString();
    }

    public Path writeAssFile(Story story, VideoTimeline timeline, Path targetPath) {
        List<SubtitleEntry> entries = buildSubtitleEntries(story, timeline);
        String assContent = generateAssContent(entries);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, assContent, StandardCharsets.UTF_8);
            log.info("Archivo ASS generado con {} subtítulos estilizados en: {}", entries.size(), targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Error al escribir archivo ASS de subtítulos: " + targetPath, e);
        }
    }

    private String formatAssTime(double seconds) {
        long totalCentis = Math.round(seconds * 100.0);
        long hrs = totalCentis / 360000;
        long mins = (totalCentis % 360000) / 6000;
        long secs = (totalCentis % 6000) / 100;
        long centis = totalCentis % 100;
        return String.format(Locale.US, "%d:%02d:%02d.%02d", hrs, mins, secs, centis);
    }

    private List<String> splitTextIntoChunks(String text, int maxWordsPerChunk) {
        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;

        for (String word : words) {
            if (word.isBlank()) continue;
            if (wordCount >= maxWordsPerChunk) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                wordCount = 0;
            }
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(word);
            wordCount++;
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private String formatSrtTime(double seconds) {
        long totalMillis = Math.round(seconds * 1000.0);
        long hrs = totalMillis / 3600000;
        long mins = (totalMillis % 3600000) / 60000;
        long secs = (totalMillis % 60000) / 1000;
        long millis = totalMillis % 1000;

        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hrs, mins, secs, millis);
    }
}
