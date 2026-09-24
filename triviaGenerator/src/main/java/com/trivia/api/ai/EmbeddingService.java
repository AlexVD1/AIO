package com.trivia.api.ai;

/**
 * Servicio para generación y cálculo de similitud vectorial de embeddings.
 *
 * Empleado en la Capa 5 de deduplicación semántica para detectar preguntas
 * que, aunque tengan palabras distintas (distinto hash SHA-256), tienen
 * exactamente el mismo significado conceptual.
 */
public interface EmbeddingService {

    /**
     * Genera el vector de embedding para un texto dado.
     *
     * @param text Texto a vectorizar
     * @return Vector de características flotantes
     */
    float[] generateEmbedding(String text);

    /**
     * Calcula la similitud coseno entre dos vectores de embedding.
     * Resultado en el rango [-1.0, 1.0], donde 1.0 indica máxima identidad semántica.
     *
     * @param vectorA Primer vector
     * @param vectorB Segundo vector
     * @return Valor de similitud coseno
     */
    double cosineSimilarity(float[] vectorA, float[] vectorB);

    /**
     * Determina si dos vectores superan el umbral de similitud semántica.
     *
     * @param vectorA Primer vector
     * @param vectorB Segundo vector
     * @param threshold Umbral mínimo (ej. 0.90)
     * @return true si la similitud es mayor o igual al umbral
     */
    boolean isSemanticallySimilar(float[] vectorA, float[] vectorB, double threshold);

    /**
     * Serializa un vector a formato texto para persistirlo en la columna 'embedding' (TEXT).
     *
     * @param vector Vector de floats
     * @return Cadena serializada (JSON o separada por comas)
     */
    String serialize(float[] vector);

    /**
     * Reconstruye un vector a partir de su representación de texto.
     *
     * @param serialized Cadena serializada
     * @return Vector de floats
     */
    float[] deserialize(String serialized);
}
