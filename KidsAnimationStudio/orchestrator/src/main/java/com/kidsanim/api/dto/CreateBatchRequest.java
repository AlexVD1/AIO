package com.kidsanim.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateBatchRequest(
        @NotNull UUID seriesId,
        String name,
        @NotEmpty List<TopicBatchItem> topics,
        Boolean autoApprove
) {
    public CreateBatchRequest {
        if (autoApprove == null) autoApprove = true;
        if (name == null || name.isBlank()) name = "Lote " + java.time.LocalDate.now();
    }
}
