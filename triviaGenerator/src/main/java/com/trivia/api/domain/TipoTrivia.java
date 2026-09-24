package com.trivia.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Catálogo de tipos de trivia disponibles en la plataforma.
 *
 * Esta entidad es el punto de entrada del sistema:
 * el cliente elige un tipo de trivia y el sistema usa ese código
 * para validar la solicitud, buscar contexto en la BD,
 * y guiar al LLM en la generación.
 *
 * El campo 'codigo' es el identificador de negocio (ej: "ASTRONOMIA").
 * El campo 'id' (UUID) es el identificador técnico de la BD.
 *
 * Por qué UUID en lugar de Long?
 *   - Distribuido: no requiere secuencia centralizada
 *   - Opaco: no revela cuántos registros existen
 *   - Portable: funciona igual en cualquier entorno
 */
@Entity
@Table(name = "tipo_trivia")
public class TipoTrivia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador de negocio único.
     * Ejemplos: CIENCIA_NATURAL, GEOLOGIA, ASTRONOMIA.
     * El cliente lo envía en el request; el backend lo valida contra este catálogo.
     */
    @Column(name = "codigo", unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /**
     * Permite desactivar un tipo de trivia sin eliminarlo.
     * Las trivias existentes de ese tipo conservan sus datos.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * @CreationTimestamp: Hibernate asigna automáticamente la fecha/hora
     * al momento de hacer el INSERT. No puede modificarse después.
     * updatable = false garantiza que Hibernate nunca intente actualizarlo.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================================================
    // Constructores
    // ========================================================

    protected TipoTrivia() {
        // Constructor vacío requerido por JPA/Hibernate.
        // protected evita uso accidental desde código de negocio.
    }

    public TipoTrivia(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = true;
    }

    // ========================================================
    // Getters y Setters
    // ========================================================

    public UUID getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    // ========================================================
    // equals / hashCode basados en el código de negocio
    // Evita depender del ID de BD que puede ser null antes de persistir
    // ========================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TipoTrivia that)) return false;
        return codigo != null && codigo.equals(that.codigo);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TipoTrivia{codigo='" + codigo + "', nombre='" + nombre + "'}";
    }
}
