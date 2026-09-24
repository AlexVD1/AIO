package com.trivia.api.exception;

/**
 * Excepción lanzada cuando el pipeline de generación agota los intentos máximos permitidos
 * (MAX_GENERATION_ATTEMPTS) sin lograr la cantidad solicitada de trivias únicas y válidas.
 */
public class InsufficientUniqueTriviasException extends RuntimeException {

    private final int solicitadas;
    private final int obtenidas;
    private final int intentos;

    public InsufficientUniqueTriviasException(int solicitadas, int obtenidas, int intentos) {
        super(String.format(
                "INSUFFICIENT_UNIQUE_TRIVIAS: No se pudo alcanzar la cantidad solicitada de %d trivias únicas. " +
                "Solo se obtuvieron %d tras %d intentos.",
                solicitadas, obtenidas, intentos));
        this.solicitadas = solicitadas;
        this.obtenidas = obtenidas;
        this.intentos = intentos;
    }

    public int getSolicitadas() {
        return solicitadas;
    }

    public int getObtenidas() {
        return obtenidas;
    }

    public int getIntentos() {
        return intentos;
    }
}
