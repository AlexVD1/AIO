package com.kidsanim.api.dto;

import com.kidsanim.api.domain.enums.EducationalTopicType;
import jakarta.validation.constraints.NotNull;

public record TopicBatchItem(
        @NotNull EducationalTopicType topicType,
        String topicDetail,
        String learningObjective,
        Integer targetDurationSec
) {
    public TopicBatchItem {
        if (targetDurationSec == null || targetDurationSec <= 0) targetDurationSec = 60;
    }
}
