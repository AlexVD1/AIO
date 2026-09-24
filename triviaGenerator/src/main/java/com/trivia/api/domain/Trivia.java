package com.trivia.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * La entidad central del sistema: una pregunta de trivia almacenada.
 *
 * IDENTIDAD DE UNA TRIVIA:
 *   La identidad está determinada por 'preguntaHash' (SHA-256 de pregunta_normalizada).
 *   La restricción UNIQUE en pregunta_hash en la BD es la última barrera
 *   contra duplicados exactos, incluso en solicitudes concurrentes.
 *
 * INMUTABILIDAD:
 *   Una vez almacenada, una trivia NO se modifica directamente.
 *   Si se detecta un error, se cambia su estado a INVALIDA o ARCHIVADA
 *   y se conserva el registro completo para trazabilidad.
 *
 * EMBEDDING (Fase 10):
 *   El campo 'embedding' está declarado como TEXT ahora.
 *   En la Fase 10 se migrará con pgvector a 'vector(1536)' para permitir
 *   búsqueda semántica por similitud coseno. La migración Flyway V10 lo hará
 *   sin necesidad de rediseñar la entidad gracias a la abstracción actual.
 */
@Entity
@Table(name = "trivia",
        indexes = {
                @Index(name = "idx_trivia_tipo_trivia", columnList = "tipo_trivia_id"),
                @Index(name = "idx_trivia_dificultad",  columnList = "dificultad"),
                @Index(name = "idx_trivia_idioma",      columnList = "idioma"),
                @Index(name = "idx_trivia_estado",      columnList = "estado"),
                @Index(name = "idx_trivia_subtema",     columnList = "subtema")
        })
public class Trivia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private TriviaGeneration generation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_trivia_id", nullable = false)
    private TipoTrivia tipoTrivia;

    /** El texto original de la pregunta tal como lo generó el LLM. */
    @Column(name = "pregunta", columnDefinition = "TEXT", nullable = false)
    private String pregunta;

    /**
     * La pregunta después de aplicar TriviaQuestionNormalizer:
     *   - Convertida a minúsculas
     *   - Espacios redundantes eliminados
     *   - Puntuación irrelevante normalizada
     *   - Acentos conservados (cambian significado en español)
     * Se usa para calcular el hash y comparar duplicados.
     */
    @Column(name = "pregunta_normalizada", columnDefinition = "TEXT", nullable = false)
    private String preguntaNormalizada;

    /**
     * SHA-256 de pregunta_normalizada en formato hexadecimal (64 caracteres).
     * Restricción UNIQUE: garantiza que no existan dos trivias con la misma pregunta
     * normalizada, incluso en inserciones concurrentes.
     *
     * Por qué SHA-256 y no MD5?
     *   SHA-256 tiene menor probabilidad de colisión (2^256 vs 2^128).
     *   Para este caso MD5 sería suficiente, pero SHA-256 es el estándar actual.
     */
    @Column(name = "pregunta_hash", nullable = false, length = 64, unique = true)
    private String preguntaHash;

    /**
     * FASE 10: Este campo será una columna 'vector(1536)' con pgvector.
     * Por ahora es TEXT (null para trivias generadas antes de la Fase 10).
     * Cuando se genere un embedding, se almacenará aquí en formato JSON o float[].
     *
     * La arquitectura está preparada para este cambio sin rediseñar la entidad:
     * solo requiere una migración Flyway que cambie el tipo de columna y
     * actualizar este campo al tipo correcto de pgvector.
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    /** Explicación de por qué la respuesta correcta es la correcta. */
    @Column(name = "explicacion", columnDefinition = "TEXT", nullable = false)
    private String explicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "dificultad", nullable = false, length = 10)
    private Dificultad dificultad;

    /** BCP 47: "es-MX", "es-ES", "en-US". Permite filtrar por idioma en consultas. */
    @Column(name = "idioma", nullable = false, length = 10)
    private String idioma;

    @Column(name = "subtema", length = 200)
    private String subtema;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoTrivia estado = EstadoTrivia.ACTIVA;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================================================
    // Constructores
    // ========================================================

    protected Trivia() {}

    public Trivia(TriviaGeneration generation, TipoTrivia tipoTrivia,
                  String pregunta, String preguntaNormalizada, String preguntaHash,
                  String explicacion, Dificultad dificultad,
                  String idioma, String subtema) {
        this.generation = generation;
        this.tipoTrivia = tipoTrivia;
        this.pregunta = pregunta;
        this.preguntaNormalizada = preguntaNormalizada;
        this.preguntaHash = preguntaHash;
        this.explicacion = explicacion;
        this.dificultad = dificultad;
        this.idioma = idioma;
        this.subtema = subtema;
        this.estado = EstadoTrivia.ACTIVA;
    }

    // ========================================================
    // Getters y Setters
    // ========================================================

    public UUID getId() { return id; }

    public TriviaGeneration getGeneration() { return generation; }

    public TipoTrivia getTipoTrivia() { return tipoTrivia; }

    public String getPregunta() { return pregunta; }

    public String getPreguntaNormalizada() { return preguntaNormalizada; }

    public String getPreguntaHash() { return preguntaHash; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public String getExplicacion() { return explicacion; }

    public Dificultad getDificultad() { return dificultad; }

    public String getIdioma() { return idioma; }

    public String getSubtema() { return subtema; }

    public EstadoTrivia getEstado() { return estado; }
    public void setEstado(EstadoTrivia estado) { this.estado = estado; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trivia that)) return false;
        return preguntaHash != null && preguntaHash.equals(that.preguntaHash);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Trivia{id=" + id + ", hash='" + preguntaHash + "', estado=" + estado + "}";
    }
}
