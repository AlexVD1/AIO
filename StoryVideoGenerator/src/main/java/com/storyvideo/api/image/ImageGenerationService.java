package com.storyvideo.api.image;

import com.storyvideo.api.image.dto.GeneratedImage;
import com.storyvideo.api.image.dto.ImageGenerationRequest;

public interface ImageGenerationService {
    GeneratedImage generate(ImageGenerationRequest request);
    boolean isAvailable();
    String getProviderName();
    int getEstimatedTimeSeconds();
}
