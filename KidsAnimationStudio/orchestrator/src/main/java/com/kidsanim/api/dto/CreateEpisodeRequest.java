package com.kidsanim.api.dto;

import com.kidsanim.api.domain.enums.EducationalTopicType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEpisodeRequest(
        @NotNull(message = "El tipo de tema educativo es obligatorio")
        EducationalTopicType topicType,

        @NotBlank(message = "El detalle del tema es obligatorio")
        String topicDetail,

        String learningObjective,

        Integer targetDurationSec,

        Boolean autoApprove
) {
    public CreateEpisodeRequest {
        if (targetDurationSec == null || targetDurationSec <= 0) targetDurationSec = 120;
        if (autoApprove == null) autoApprove = true;
    }
}
