package com.kidsanim.api.infrastructure.exception;

public class AiGatewayException extends RuntimeException {
    private final Integer statusCode;

    public AiGatewayException(String message) {
        super(message);
        this.statusCode = null;
    }

    public AiGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public AiGatewayException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
