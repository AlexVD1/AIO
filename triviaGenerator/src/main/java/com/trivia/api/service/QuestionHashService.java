package com.trivia.api.service;

import com.trivia.api.normalizer.TriviaQuestionNormalizer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Servicio encargado de calcular el hash criptográfico SHA-256 de las preguntas.
 *
 * PROPÓSITO:
 *   Produce un identificador determinista de 64 caracteres en hexadecimal a partir
 *   de una pregunta normalizada. Este hash alimenta la columna 'pregunta_hash' con
 *   restricción UNIQUE en la base de datos (PostgreSQL), impidiendo duplicados exactos.
 *
 * POR QUÉ SHA-256:
 *   - 256 bits de entropía (prácticamente cero riesgo de colisión accidental).
 *   - Salida fija de 64 caracteres hexadecimales (cabe exactamente en VARCHAR(64)).
 *   - Estándar de la industria y soportado nativamente en el JDK sin librerías externas.
 */
@Service
public class QuestionHashService {

    private final TriviaQuestionNormalizer normalizer;

    public QuestionHashService(TriviaQuestionNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * Calcula el hash SHA-256 de un texto dado.
     *
     * @param input Texto de entrada
     * @return Hash SHA-256 en formato hexadecimal en minúsculas (64 caracteres)
     * @throws IllegalArgumentException si el texto es nulo
     */
    public String calculateHash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("El texto para calcular el hash no puede ser nulo.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM.", e);
        }
    }

    /**
     * Normaliza la pregunta original y calcula su hash SHA-256.
     *
     * Flujo:
     *   Pregunta cruda -> TriviaQuestionNormalizer -> SHA-256 Hex
     *
     * @param rawQuestion Pregunta tal como viene del LLM o usuario
     * @return Hash SHA-256 de la pregunta normalizada
     */
    public String hashQuestion(String rawQuestion) {
        String normalized = normalizer.normalize(rawQuestion);
        return calculateHash(normalized);
    }

    /**
     * Obtiene la versión normalizada de una pregunta delegando en el normalizador.
     *
     * @param rawQuestion Pregunta original
     * @return Pregunta normalizada
     */
    public String normalize(String rawQuestion) {
        return normalizer.normalize(rawQuestion);
    }
}
