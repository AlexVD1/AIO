package com.kidsanim.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kids.safety")
public record KidsSafetyProperties(
        String blocklistPath,
        int maxNarrationWords,
        int minScenes,
        int maxScenes,
        int minShots,
        int maxShots,
        int maxPedagogicalRetries
) {
    public KidsSafetyProperties {
        if (blocklistPath == null || blocklistPath.isBlank()) {
            blocklistPath = "classpath:content-safety-blocklist.txt";
        }
        if (maxNarrationWords <= 0) maxNarrationWords = 14;
        if (minScenes <= 0) minScenes = 4;
        if (maxScenes <= 0) maxScenes = 8;
        if (minShots <= 0) minShots = 12;
        if (maxShots <= 0) maxShots = 40;
        if (maxPedagogicalRetries <= 0) maxPedagogicalRetries = 2;
    }
}
