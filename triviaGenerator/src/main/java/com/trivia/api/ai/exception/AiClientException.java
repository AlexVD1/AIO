package com.trivia.api.ai.exception;

/**
 * Excepción lanzada cuando ocurre un error de comunicación, autenticación o formato
 * con el proveedor de Inteligencia Artificial.
 */
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
        super(message + " (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
