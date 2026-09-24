package com.trivia.api.service;

import com.trivia.api.ai.dto.AiTriviaItem;
import com.trivia.api.ai.dto.AiTriviaOpcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Validador de integridad para trivias generadas por el modelo de IA.
 *
 * REGLAS DE VALIDACIÓN:
 *   1. La pregunta no puede ser nula ni estar en blanco.
 *   2. La explicación no puede ser nula ni estar en blanco.
 *   3. La cantidad de opciones debe coincidir exactamente con el número solicitado (entre 2 y 8).
 *   4. Las letras de las opciones deben ser consecutivas: A, B, C, D...
 *   5. El texto de cada opción no puede estar en blanco.
 *   6. No pueden existir opciones con texto duplicado dentro de la misma pregunta.
 *   7. Exactamente UNA opción debe tener 'correcta' = true.
 */
@Service
public class TriviaValidationService {

    private static final Logger log = LoggerFactory.getLogger(TriviaValidationService.class);

    /**
     * Evalúa si una trivia generada por la IA cumple con todas las reglas de negocio.
     *
     * @param item Trivia candidata
     * @param numeroOpcionesEsperadas Cantidad esperada de opciones (2 a 8)
     * @return true si es completamente válida, false si incumple alguna regla
     */
    public boolean isValid(AiTriviaItem item, int numeroOpcionesEsperadas) {
        if (item == null) {
            log.debug("Trivia descartada: item nulo");
            return false;
        }

        if (item.pregunta() == null || item.pregunta().isBlank()) {
            log.debug("Trivia descartada: pregunta nula o en blanco");
            return false;
        }

        if (item.explicacion() == null || item.explicacion().isBlank()) {
            log.debug("Trivia descartada: explicación nula o en blanco: '{}'", item.pregunta());
            return false;
        }

        List<AiTriviaOpcion> opciones = item.opciones();
        if (opciones == null || opciones.size() != numeroOpcionesEsperadas) {
            log.debug("Trivia descartada: cantidad de opciones incorrecta (esperadas {}, obtenidas {}): '{}'",
                    numeroOpcionesEsperadas, opciones == null ? 0 : opciones.size(), item.pregunta());
            return false;
        }

        int correctasCount = 0;
        Set<String> textosOpciones = new HashSet<>();

        for (int i = 0; i < opciones.size(); i++) {
            AiTriviaOpcion opcion = opciones.get(i);
            if (opcion == null) {
                log.debug("Trivia descartada: opción nula en índice {}", i);
                return false;
            }

            char letraEsperada = (char) ('A' + i);
            if (opcion.letra() == null || !opcion.letra().equalsIgnoreCase(String.valueOf(letraEsperada))) {
                log.debug("Trivia descartada: letra de opción incorrecta (esperada '{}', obtenida '{}')",
                        letraEsperada, opcion.letra());
                return false;
            }

            if (opcion.texto() == null || opcion.texto().isBlank()) {
                log.debug("Trivia descartada: texto de opción vacío para letra '{}'", letraEsperada);
                return false;
            }

            String textoNormalizado = opcion.texto().trim().toLowerCase(Locale.ROOT);
            if (!textosOpciones.add(textoNormalizado)) {
                log.debug("Trivia descartada: texto de opción duplicado dentro de la misma trivia: '{}'", opcion.texto());
                return false;
            }

            if (Boolean.TRUE.equals(opcion.correcta())) {
                correctasCount++;
            }
        }

        if (correctasCount != 1) {
            log.debug("Trivia descartada: debe tener exactamente 1 opción correcta (tiene {}): '{}'",
                    correctasCount, item.pregunta());
            return false;
        }

        return true;
    }

    /**
     * Filtra una lista de trivias candidatas conservando únicamente las que son válidas.
     *
     * @param items Lista de trivias de la IA
     * @param numeroOpcionesEsperadas Cantidad esperada de opciones
     * @return Lista de trivias válidas
     */
    public List<AiTriviaItem> filterValid(List<AiTriviaItem> items, int numeroOpcionesEsperadas) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<AiTriviaItem> validas = new ArrayList<>();
        for (AiTriviaItem item : items) {
            if (isValid(item, numeroOpcionesEsperadas)) {
                validas.add(item);
            }
        }
        return validas;
    }
}
