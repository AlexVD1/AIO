package com.trivia.api.dto;

import java.util.Map;

/**
 * DTO que consolida las estadísticas globales de la plataforma de trivias.
 */
public record StatsResponse(
        long totalTriviasActivas,
        long totalGeneraciones,
        Double promedioIntentosPorGeneracion,
        Double promedioDescartadasPorGeneracion,
        Map<String, Long> triviasPorDificultad
) {
}
