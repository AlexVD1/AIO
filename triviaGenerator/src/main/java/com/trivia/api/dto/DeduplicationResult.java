package com.trivia.api.dto;

import java.util.List;

/**
 * Resultado estructurado del proceso de deduplicación de trivias.
 *
 * Proporciona trazabilidad completa sobre cuántas y cuáles preguntas
 * fueron aceptadas o descartadas por cada causa (duplicado intra-lote vs base de datos).
 *
 * @param <T> Tipo del elemento evaluado (String, DTO candidato, etc.)
 */
public record DeduplicationResult<T>(
        List<T> validos,
        List<T> descartadosPorBatch,
        List<T> descartadosPorBaseDeDatos
) {
    public int totalDescartados() {
        return descartadosPorBatch.size() + descartadosPorBaseDeDatos.size();
    }

    public int totalValidos() {
        return validos.size();
    }

    public boolean tieneDescartados() {
        return totalDescartados() > 0;
    }
}
