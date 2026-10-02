package com.storyvideo.api.ai.exception;

public class AiClientException extends RuntimeException {

    private final Integer statusCode;

    public AiClientException(String message) {
        super(message);
        this.statusCode = null;
    }

    public AiClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public AiClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
