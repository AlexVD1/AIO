package com.kidsanim.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kids.pipeline")
public record KidsPipelineProperties(
        String storagePath,
        String defaultNarratorVoice,
        String defaultNarratorRate,
        Double characterSimilarityMin,
        Integer maxKeyframeRetries
) {
    public KidsPipelineProperties {
        if (storagePath == null || storagePath.isBlank()) storagePath = "./storage";
        if (defaultNarratorVoice == null || defaultNarratorVoice.isBlank()) defaultNarratorVoice = "ef_dora";
        if (defaultNarratorRate == null || defaultNarratorRate.isBlank()) defaultNarratorRate = "+0%";
        if (characterSimilarityMin == null) characterSimilarityMin = 0.80;
        if (maxKeyframeRetries == null || maxKeyframeRetries <= 0) maxKeyframeRetries = 3;
    }

    public KidsPipelineProperties() {
        this(null, null, null, null, null);
    }

    public KidsPipelineProperties(String storagePath, String defaultNarratorVoice, String defaultNarratorRate) {
        this(storagePath, defaultNarratorVoice, defaultNarratorRate, 0.80, 3);
    }
}
