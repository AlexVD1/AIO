package com.kidsanim.api.llm;

import java.util.Map;

public interface LlmProvider {
    String generateStructured(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema);
    boolean isAvailable();
    String getProviderName();
}
