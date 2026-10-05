package com.kidsanim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproveCharacterRequest(
        @NotBlank(message = "La ruta de la imagen aprobada es obligatoria")
        String imagePath,

        @NotNull(message = "La seed de referencia es obligatoria")
        Long seed
) {
}
