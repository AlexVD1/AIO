package com.kidsanim.api.infrastructure.exception;

import java.util.List;

public class ScriptValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public ScriptValidationException(String message, List<String> validationErrors) {
        super(message + ": " + String.join("; ", validationErrors));
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
