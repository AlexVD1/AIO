package com.storyvideo.api.audio.narration;

import com.storyvideo.api.audio.narration.dto.NarrationRequest;
import com.storyvideo.api.audio.narration.dto.NarrationResult;

public interface NarrationService {
    NarrationResult synthesize(NarrationRequest request);
    boolean isAvailable();
    String getProviderName();
}
