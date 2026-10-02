package com.storyvideo.api.ai;

import com.storyvideo.api.ai.dto.AiStoryGenerationRequest;
import com.storyvideo.api.ai.dto.AiStoryResponse;

public interface StoryAiClient {
    AiStoryResponse generateStory(AiStoryGenerationRequest request);
    boolean isAvailable();
}
