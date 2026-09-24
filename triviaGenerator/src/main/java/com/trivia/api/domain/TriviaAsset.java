package com.trivia.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asset (imagen PNG) asociado a una trivia.
 *
 * Cada trivia tiene exactamente DOS assets:
 *   - TipoAsset.PREGUNTA: imagen con la pregunta sin revelar respuesta
 *   - TipoAsset.RESPUESTA: imagen con la respuesta correcta destacada
 *
 * ABSTRACCIÓN DE ALMACENAMIENTO:
 *   El campo 'path' guarda la ruta relativa en el filesystem (o clave en S3/R2).
 *   El campo 'url' guarda la URL pública accesible desde el exterior.
 *
 *   Esta separación permite cambiar el backend de almacenamiento (Fase 12):
 *   - Filesystem local → S3 → Cloudflare R2
 *   Sin necesidad de modificar la entidad. Solo cambia AssetStorageService.
 *
 *   Ejemplo (filesystem):
 *     path = "2026/09/generation-uuid/trivia-001-question.png"
 *     url  = "https://api.example.com/assets/2026/09/generation-uuid/trivia-001-question.png"
 *
 *   Ejemplo (S3):
 *     path = "s3://bucket/2026/09/generation-uuid/trivia-001-question.png"
 *     url  = "https://cdn.example.com/2026/09/generation-uuid/trivia-001-question.png"
 */
@Entity
@Table(name = "trivia_asset",
        indexes = {
                @Index(name = "idx_trivia_asset_trivia", columnList = "trivia_id")
        })
public class TriviaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trivia_id", nullable = false)
    private Trivia trivia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoAsset tipo;

    /** Ruta relativa o clave del asset en el backend de almacenamiento. */
    @Column(name = "path", columnDefinition = "TEXT", nullable = false)
    private String path;

    /** URL pública con la que el cliente puede acceder a la imagen. */
    @Column(name = "url", columnDefinition = "TEXT", nullable = false)
    private String url;

    /** Por defecto "image/png" ya que solo generamos PNGs. */
    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType = "image/png";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================================================
    // Constructores
    // ========================================================

    protected TriviaAsset() {}

    public TriviaAsset(Trivia trivia, TipoAsset tipo, String path, String url) {
        this.trivia = trivia;
        this.tipo = tipo;
        this.path = path;
        this.url = url;
        this.mimeType = "image/png";
    }

    // ========================================================
    // Getters
    // ========================================================

    public UUID getId() { return id; }

    public Trivia getTrivia() { return trivia; }

    public TipoAsset getTipo() { return tipo; }

    public String getPath() { return path; }

    public String getUrl() { return url; }

    public String getMimeType() { return mimeType; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "TriviaAsset{tipo=" + tipo + ", url='" + url + "'}";
    }
}
