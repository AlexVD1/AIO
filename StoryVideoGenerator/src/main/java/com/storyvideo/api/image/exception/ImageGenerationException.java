package com.storyvideo.api.image.exception;

public class ImageGenerationException extends RuntimeException {

    private final Integer statusCode;

    public ImageGenerationException(String message) {
        super(message);
        this.statusCode = null;
    }

    public ImageGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public ImageGenerationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
