package com.kidsanim.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kids.llm")
public record KidsLlmProperties(
        String provider,
        OllamaConfig ollama,
        GeminiConfig gemini
) {
    public KidsLlmProperties {
        if (provider == null || provider.isBlank()) provider = "ollama";
        if (ollama == null) ollama = new OllamaConfig("qwen2.5:7b-instruct", 0.7, 120);
        if (gemini == null) gemini = new GeminiConfig("", "gemini-2.5-flash", 60);
    }

    public record OllamaConfig(
            String model,
            double temperature,
            int timeoutSeconds
    ) {
        public OllamaConfig {
            if (model == null || model.isBlank()) model = "qwen2.5:7b-instruct";
            if (temperature <= 0) temperature = 0.7;
            if (timeoutSeconds <= 0) timeoutSeconds = 120;
        }
    }

    public record GeminiConfig(
            String apiKey,
            String model,
            int timeoutSeconds
    ) {
        public GeminiConfig {
            if (apiKey == null) apiKey = "";
            if (model == null || model.isBlank()) model = "gemini-2.5-flash";
            if (timeoutSeconds <= 0) timeoutSeconds = 60;
        }
    }
}
