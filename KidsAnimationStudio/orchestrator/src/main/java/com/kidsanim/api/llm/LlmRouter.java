package com.kidsanim.api.llm;

import com.kidsanim.api.infrastructure.config.KidsLlmProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Primary
public class LlmRouter implements LlmProvider {

    private final LlmProvider ollamaProvider;
    private final LlmProvider geminiProvider;
    private final String selectedProviderName;

    public LlmRouter(OllamaLlmProvider ollamaProvider,
                     GeminiLlmProvider geminiProvider,
                     KidsLlmProperties llmProperties) {
        this.ollamaProvider = ollamaProvider;
        this.geminiProvider = geminiProvider;
        this.selectedProviderName = llmProperties.provider() != null
                ? llmProperties.provider().trim().toLowerCase()
                : "ollama";
    }

    public LlmProvider getActiveProvider() {
        if ("gemini".equalsIgnoreCase(selectedProviderName)) {
            return geminiProvider;
        }
        return ollamaProvider;
    }

    @Override
    public String generateStructured(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        return getActiveProvider().generateStructured(systemPrompt, userPrompt, jsonSchema);
    }

    @Override
    public boolean isAvailable() {
        return getActiveProvider().isAvailable();
    }

    @Override
    public String getProviderName() {
        return getActiveProvider().getProviderName();
    }
}
