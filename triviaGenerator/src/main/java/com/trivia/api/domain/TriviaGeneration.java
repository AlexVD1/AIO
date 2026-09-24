package com.trivia.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una solicitud de generación de trivias.
 *
 * Esta entidad cumple dos funciones:
 * 1. TRAZABILIDAD: Registra cada solicitud POST /api/v1/trivias con todos sus parámetros.
 * 2. OBSERVABILIDAD: Almacena contadores, intentos, duración y estado para monitoreo.
 *
 * El ciclo de vida sigue el enum EstadoGeneracion:
 *   PENDIENTE → GENERANDO → ... → COMPLETADA | ERROR
 *
 * Por qué no usar un simple booleano "completado"?
 *   Los estados intermedios permiten saber exactamente en qué paso
 *   falló una generación, cuánto tardó cada fase, y cuántas trivias
 *   se descartaron en cada intento. Esto es esencial para diagnosticar
 *   problemas y ajustar el buffer de generación.
 */
@Entity
@Table(name = "trivia_generation",
        indexes = {
                @Index(name = "idx_trivia_generation_estado", columnList = "estado"),
                @Index(name = "idx_trivia_generation_tipo_trivia", columnList = "tipo_trivia_id")
        })
public class TriviaGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * FetchType.LAZY: Hibernate NO carga TipoTrivia automáticamente cuando
     * carga TriviaGeneration. Solo lo carga si se accede a tipoTrivia.
     * Esto evita JOINs innecesarios en operaciones que no necesitan el catálogo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_trivia_id", nullable = false)
    private TipoTrivia tipoTrivia;

    @Column(name = "subtema", length = 200)
    private String subtema;

    /** Cantidad que el usuario pidió en el request. */
    @Column(name = "cantidad_solicitada", nullable = false)
    private Integer cantidadSolicitada;

    /** Total de trivias que el LLM generó en todos los intentos. */
    @Column(name = "cantidad_generada", nullable = false)
    private Integer cantidadGenerada = 0;

    /** Total de trivias descartadas (duplicadas + inválidas). */
    @Column(name = "cantidad_descartada", nullable = false)
    private Integer cantidadDescartada = 0;

    /** Trivias válidas efectivamente almacenadas al final. */
    @Column(name = "cantidad_final", nullable = false)
    private Integer cantidadFinal = 0;

    @Column(name = "numero_opciones", nullable = false)
    private Integer numeroOpciones;

    /**
     * @Enumerated(EnumType.STRING): Hibernate guarda el NOMBRE del enum ("FACIL", "MEDIA", "DIFICIL")
     * en la columna VARCHAR. Alternativa descartada: EnumType.ORDINAL guarda el número (0, 1, 2)
     * pero es frágil ante reordenamientos del enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dificultad", nullable = false, length = 10)
    private Dificultad dificultad;

    @Column(name = "idioma", nullable = false, length = 10)
    private String idioma;

    /** Número de ciclos de generación realizados (máx: MAX_GENERATION_ATTEMPTS). */
    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoGeneracion estado = EstadoGeneracion.PENDIENTE;

    /** Mensaje de error en caso de fallo. Se guarda para diagnóstico. */
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Fecha/hora de finalización (COMPLETADA o ERROR). */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ========================================================
    // Constructores
    // ========================================================

    protected TriviaGeneration() {}

    public TriviaGeneration(TipoTrivia tipoTrivia, Integer cantidadSolicitada,
                            Integer numeroOpciones, Dificultad dificultad,
                            String idioma, String subtema) {
        this.tipoTrivia = tipoTrivia;
        this.cantidadSolicitada = cantidadSolicitada;
        this.numeroOpciones = numeroOpciones;
        this.dificultad = dificultad;
        this.idioma = idioma;
        this.subtema = subtema;
        this.estado = EstadoGeneracion.PENDIENTE;
    }

    // ========================================================
    // Métodos de dominio: cambian el estado con lógica explícita
    // ========================================================

    /** Incrementa el contador de intentos. Llamar antes de cada ciclo LLM. */
    public void incrementarIntento() {
        this.intentos++;
    }

    /** Registra el estado como COMPLETADA con timestamp actual. */
    public void completar(int cantidadFinal) {
        this.cantidadFinal = cantidadFinal;
        this.estado = EstadoGeneracion.COMPLETADA;
        this.completedAt = LocalDateTime.now();
    }

    /** Registra el estado como ERROR con mensaje y timestamp actual. */
    public void fallar(String mensajeError) {
        this.error = mensajeError;
        this.estado = EstadoGeneracion.ERROR;
        this.completedAt = LocalDateTime.now();
    }

    // ========================================================
    // Getters y Setters
    // ========================================================

    public UUID getId() { return id; }

    public TipoTrivia getTipoTrivia() { return tipoTrivia; }

    public String getSubtema() { return subtema; }

    public Integer getCantidadSolicitada() { return cantidadSolicitada; }

    public Integer getCantidadGenerada() { return cantidadGenerada; }
    public void setCantidadGenerada(Integer cantidadGenerada) { this.cantidadGenerada = cantidadGenerada; }

    public Integer getCantidadDescartada() { return cantidadDescartada; }
    public void setCantidadDescartada(Integer cantidadDescartada) { this.cantidadDescartada = cantidadDescartada; }

    public Integer getCantidadFinal() { return cantidadFinal; }

    public Integer getNumeroOpciones() { return numeroOpciones; }

    public Dificultad getDificultad() { return dificultad; }

    public String getIdioma() { return idioma; }

    public Integer getIntentos() { return intentos; }

    public EstadoGeneracion getEstado() { return estado; }
    public void setEstado(EstadoGeneracion estado) { this.estado = estado; }

    public String getError() { return error; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }

    @Override
    public String toString() {
        return "TriviaGeneration{id=" + id + ", estado=" + estado +
                ", solicitada=" + cantidadSolicitada + ", final=" + cantidadFinal + "}";
    }
}
