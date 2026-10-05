package com.kidsanim.api.infrastructure.exception;

public class LlmException extends RuntimeException {
    private final String provider;

    public LlmException(String message, String provider) {
        super(message);
        this.provider = provider;
    }

    public LlmException(String message, String provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
