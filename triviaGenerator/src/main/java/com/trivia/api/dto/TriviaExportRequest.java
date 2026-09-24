package com.trivia.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/**
 * Solicitud de exportación múltiple de trivias en formato ZIP.
 */
public record TriviaExportRequest(
        @NotEmpty(message = "La lista de IDs no puede estar vacía")
        List<UUID> triviaIds
) {}
