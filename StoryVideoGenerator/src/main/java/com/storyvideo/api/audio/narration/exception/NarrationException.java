package com.storyvideo.api.audio.narration.exception;

public class NarrationException extends RuntimeException {

    public NarrationException(String message) {
        super(message);
    }

    public NarrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
