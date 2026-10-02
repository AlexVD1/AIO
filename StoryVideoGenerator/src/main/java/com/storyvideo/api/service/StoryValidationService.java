package com.storyvideo.api.service;

import com.storyvideo.api.ai.dto.AiStoryResponse;
import com.storyvideo.api.ai.dto.AiSceneItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoryValidationService {

    private static final Logger log = LoggerFactory.getLogger(StoryValidationService.class);

    public record ValidationResult(boolean isValid, List<String> errors) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }

    public ValidationResult validate(AiStoryResponse story) {
        List<String> errors = new ArrayList<>();

        if (story == null) {
            return ValidationResult.invalid(List.of("La respuesta de la historia es nula"));
        }

        if (story.title() == null || story.title().trim().length() < 3) {
            errors.add("El título de la historia es inválido o demasiado corto");
        }

        if (story.hook() == null || story.hook().trim().length() < 8) {
            errors.add("El hook inicial es obligatorio y debe tener al menos 8 caracteres para asegurar retención");
        }

        if (story.premise() == null || story.premise().trim().isBlank()) {
            errors.add("La premisa de la historia no puede estar vacía");
        }

        if (story.synopsis() == null || story.synopsis().trim().isBlank()) {
            errors.add("La sinopsis no puede estar vacía");
        }

        if (story.ending() == null || story.ending().trim().isBlank()) {
            errors.add("El desenlace/cierre no puede estar vacío");
        }

        if (story.scenes() == null || story.scenes().isEmpty()) {
            errors.add("La historia debe contener al menos una escena");
        } else {
            if (story.scenes().size() < 3) {
                errors.add("La historia debe contener al menos 3 escenas para conformar un arco narrativo mínimo");
            }

            int expectedSeq = 1;
            for (AiSceneItem scene : story.scenes()) {
                if (scene.narrationText() == null || scene.narrationText().trim().isBlank()) {
                    errors.add("La escena " + expectedSeq + " no tiene texto de locución (narrationText)");
                }
                if (scene.visualPrompt() == null || scene.visualPrompt().trim().length() < 10) {
                    errors.add("La escena " + expectedSeq + " tiene un visualPrompt vacío o insuficiente para Stable Diffusion");
                }
                expectedSeq++;
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Historia '{}' rechazada por validación: {}", story.title(), errors);
            return ValidationResult.invalid(errors);
        }

        return ValidationResult.valid();
    }
}
