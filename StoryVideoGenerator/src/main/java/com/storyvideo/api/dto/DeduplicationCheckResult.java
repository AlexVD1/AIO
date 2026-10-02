package com.storyvideo.api.dto;

public record DeduplicationCheckResult(
        boolean duplicate,
        String reason,
        String matchedTitle
) {
    public static DeduplicationCheckResult ok() {
        return new DeduplicationCheckResult(false, null, null);
    }

    public static DeduplicationCheckResult duplicate(String reason, String matchedTitle) {
        return new DeduplicationCheckResult(true, reason, matchedTitle);
    }
}
