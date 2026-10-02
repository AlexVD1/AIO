package com.storyvideo.api.pipeline.dto;

public enum PipelineStage {
    NOT_STARTED,
    GENERATING_STORY,
    GENERATING_IMAGES,
    GENERATING_AUDIO,
    RENDERING_VIDEO,
    EXPORTING,
    COMPLETED,
    FAILED
}
