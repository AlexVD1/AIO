package com.storyvideo.api.dto;

import com.storyvideo.api.domain.NarrativeArc;
import com.storyvideo.api.domain.Story;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.domain.StoryTone;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StoryResponseDto(
        UUID id,
        String title,
        StoryGenre genre,
        String subgenre,
        StoryTone tone,
        String theme,
        String hook,
        String premise,
        String synopsis,
        NarrativeArc narrativeArc,
        String twist,
        String ending,
        String language,
        int targetDurationSeconds,
        StoryStatus status,
        int version,
        String visualStyleName,
        List<StoryCharacterResponseDto> characters,
        List<StorySceneResponseDto> scenes,
        LocalDateTime createdAt
) {
    public static StoryResponseDto fromEntity(Story story, AssetStorageService storageService) {
        String styleName = story.getVisualStyle() != null ? story.getVisualStyle().getName() : null;

        List<StoryCharacterResponseDto> chars = story.getCharacters().stream()
                .map(StoryCharacterResponseDto::fromEntity)
                .toList();

        List<StorySceneResponseDto> scenes = story.getScenes().stream()
                .map(s -> StorySceneResponseDto.fromEntity(s, storageService))
                .toList();

        return new StoryResponseDto(
                story.getId(),
                story.getTitle(),
                story.getGenre(),
                story.getSubgenre(),
                story.getTone(),
                story.getTheme(),
                story.getHook(),
                story.getPremise(),
                story.getSynopsis(),
                story.getNarrativeArc(),
                story.getTwist(),
                story.getEnding(),
                story.getLanguage(),
                story.getTargetDurationSeconds(),
                story.getStatus(),
                story.getVersion(),
                styleName,
                chars,
                scenes,
                story.getCreatedAt()
        );
    }
}
