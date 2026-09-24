package com.trivia.api.service;

/**
 * Abstracción para el almacenamiento y recuperación de assets (imágenes PNG).
 *
 * Desacopla la lógica de renderizado del proveedor de almacenamiento subyacente
 * (Filesystem local, AWS S3, Cloudflare R2, MinIO, etc.).
 */
public interface AssetStorageService {

    /**
     * Almacena un archivo binario y retorna su URL pública accesible.
     *
     * @param data Contenido binario del archivo
     * @param relativePath Ruta relativa de almacenamiento (ej: "2026/09/uuid/question.png")
     * @param mimeType Tipo MIME (ej: "image/png")
     * @return URL pública completa para acceder al recurso
     */
    String store(byte[] data, String relativePath, String mimeType);

    /**
     * Recupera el contenido binario de un archivo almacenado.
     *
     * @param relativePath Ruta relativa del archivo
     * @return Bytes del archivo
     */
    byte[] retrieve(String relativePath);

    /**
     * Verifica si un archivo existe en el almacenamiento.
     *
     * @param relativePath Ruta relativa
     * @return true si existe
     */
    boolean exists(String relativePath);

    /**
     * Elimina un archivo del almacenamiento.
     *
     * @param relativePath Ruta relativa
     * @return true si fue eliminado exitosamente
     */
    boolean delete(String relativePath);
}
