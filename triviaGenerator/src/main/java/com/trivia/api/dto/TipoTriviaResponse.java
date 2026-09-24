package com.trivia.api.dto;

import java.util.UUID;

/**
 * DTO de respuesta para un tipo de trivia en el catálogo.
 *
 * Por qué un DTO separado y no la entidad TipoTrivia directamente?
 *   1. La entidad tiene campos internos que no deben exponerse (created_at).
 *   2. Un DTO es un contrato estable: permite cambiar la entidad sin
 *      afectar el contrato público de la API.
 *   3. Jackson serializa Records a JSON automáticamente:
 *      {id: "...", codigo: "ASTRONOMIA", nombre: "Astronomía", descripcion: "..."}
 *
 * Por qué Java Record y no una clase normal?
 *   Records son inmutables por diseño (getters, equals, hashCode, toString auto-generados).
 *   Perfectos para DTOs de solo lectura que se serializan a JSON.
 */
public record TipoTriviaResponse(
        UUID id,
        String codigo,
        String nombre,
        String descripcion
) {
    /**
     * Factory method que convierte la entidad JPA al DTO.
     *
     * Por qué aquí y no en un Mapper separado?
     *   En fases tempranas, un mapper es over-engineering.
     *   Cuando tengamos 3+ DTOs por entidad, crearemos TipoTriviaMapper.
     *
     * @param entidad Entidad JPA cargada desde la BD
     * @return DTO listo para serializar a JSON
     */
    public static TipoTriviaResponse from(com.trivia.api.domain.TipoTrivia entidad) {
        return new TipoTriviaResponse(
                entidad.getId(),
                entidad.getCodigo(),
                entidad.getNombre(),
                entidad.getDescripcion()
        );
    }
}
