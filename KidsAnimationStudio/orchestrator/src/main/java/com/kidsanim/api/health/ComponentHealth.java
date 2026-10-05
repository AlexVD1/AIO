package com.kidsanim.api.health;

/**
 * Estado de un componente individual (DB, ai-gateway, ComfyUI, Ollama).
 */
public record ComponentHealth(
        String status,
        String target,
        Long latencyMs,
        String detail
) {
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";

    public boolean isUp() {
        return UP.equals(status);
    }

    public static ComponentHealth up(String target, long latencyMs, String detail) {
        return new ComponentHealth(UP, target, latencyMs, detail);
    }

    public static ComponentHealth down(String target, String detail) {
        return new ComponentHealth(DOWN, target, null, detail);
    }
}
