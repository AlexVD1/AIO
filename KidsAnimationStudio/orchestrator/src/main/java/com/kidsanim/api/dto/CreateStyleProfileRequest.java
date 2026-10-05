package com.kidsanim.api.dto;

import com.kidsanim.api.domain.enums.VideoModel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateStyleProfileRequest(
        @NotBlank(message = "El nombre del perfil de estilo es obligatorio")
        String name,

        @NotBlank(message = "El checkpoint del modelo es obligatorio")
        String checkpointName,

        @NotBlank(message = "El prompt de estilo es obligatorio")
        String stylePrompt,

        @NotBlank(message = "El negative prompt es obligatorio")
        String negativePrompt,

        String sampler,

        @Min(value = 1, message = "Los pasos deben ser al menos 1")
        Integer steps,

        @NotNull(message = "El valor CFG es obligatorio")
        BigDecimal cfg,

        @Min(value = 256, message = "El ancho mínimo es 256")
        Integer width,

        @Min(value = 256, message = "El alto mínimo es 256")
        Integer height,

        VideoModel videoModel,

        Integer videoFps,

        String videoResolution
) {
    public CreateStyleProfileRequest {
        if (sampler == null || sampler.isBlank()) sampler = "dpmpp_sde";
        if (steps == null) steps = 8;
        if (cfg == null) cfg = new BigDecimal("2.00");
        if (width == null) width = 1024;
        if (height == null) height = 576;
        if (videoModel == null) videoModel = VideoModel.LTX;
        if (videoFps == null) videoFps = 24;
        if (videoResolution == null || videoResolution.isBlank()) videoResolution = "1024x576";
    }
}
