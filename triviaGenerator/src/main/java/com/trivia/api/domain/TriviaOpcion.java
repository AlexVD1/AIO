package com.trivia.api.domain;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Una opción de respuesta dentro de una trivia.
 *
 * Una trivia tiene entre 2 y 8 opciones (según numeroDeOpciones del request).
 * Exactamente UNA opción debe tener correcta = true.
 * Esto se valida en TriviaValidator antes de persistir.
 *
 * Las letras son: A, B, C, D, E, F, G, H (según la cantidad de opciones).
 * La restricción UNIQUE (trivia_id, letra) evita duplicados de letra por trivia.
 *
 * No tiene @CreationTimestamp porque el ciclo de vida de una opción
 * está completamente ligado a su trivia: se crean y eliminan juntas.
 * (Aunque por inmutabilidad, nunca se eliminan.)
 */
@Entity
@Table(name = "trivia_opcion",
        uniqueConstraints = {
                // Una letra no puede repetirse dentro de la misma trivia
                @UniqueConstraint(name = "uq_trivia_opcion_trivia_letra",
                        columnNames = {"trivia_id", "letra"})
        },
        indexes = {
                @Index(name = "idx_trivia_opcion_trivia", columnList = "trivia_id")
        })
public class TriviaOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * FetchType.LAZY: Evita cargar la Trivia completa (con todas sus opciones)
     * cuando solo necesitamos una opción individual.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trivia_id", nullable = false)
    private Trivia trivia;

    /**
     * Letra identificadora de la opción: "A", "B", "C", "D"...
     * Se genera secuencialmente comenzando en "A".
     * LENGTH = 1: restricción en BD (no solo en Java).
     */
    @Column(name = "letra", nullable = false, length = 1)
    private String letra;

    @Column(name = "texto", columnDefinition = "TEXT", nullable = false)
    private String texto;

    /**
     * Exactamente UNA opción por trivia debe tener correcta = true.
     * Esta regla la garantiza TriviaValidator; la BD no tiene esta restricción
     * (sería costosa y compleja de implementar como CHECK en PostgreSQL).
     */
    @Column(name = "correcta", nullable = false)
    private Boolean correcta = false;

    // ========================================================
    // Constructores
    // ========================================================

    protected TriviaOpcion() {}

    public TriviaOpcion(Trivia trivia, String letra, String texto, Boolean correcta) {
        this.trivia = trivia;
        this.letra = letra;
        this.texto = texto;
        this.correcta = correcta;
    }

    // ========================================================
    // Getters
    // ========================================================

    public UUID getId() { return id; }

    public Trivia getTrivia() { return trivia; }

    public String getLetra() { return letra; }

    public String getTexto() { return texto; }

    public Boolean getCorrecta() { return correcta; }

    @Override
    public String toString() {
        return "TriviaOpcion{letra='" + letra + "', correcta=" + correcta + "}";
    }
}
