package com.trivia.api.service.video.narration;

import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.service.video.TimelinePlan;
import com.trivia.api.service.video.VideoFormat;

import java.util.List;
import java.util.Map;

/**
 * Contrato para la planificación de locución y cálculo de tiempos de escena.
 *
 * Permite alternar de manera transparente entre:
 *   - Modo sin TTS (tiempos fijos estándar para lectura visual).
 *   - Modo con TTS dinámico (duraciones calculadas según la longitud de la voz sintética).
 */
public interface NarrationService {

    /**
     * Construye el plan de tiempos para la lista de trivias dada (sin TTS ni intro).
     */
    default TimelinePlan planTimeline(List<Trivia> trivias, VideoFormat format, boolean withBgm) {
        return planTimeline(trivias, Map.of(), format, withBgm, false, "es-MX-JorgeNeural", null, null);
    }

    /**
     * Construye el plan de tiempos para la lista de trivias dada con soporte opcional de locución TTS (sin intro).
     */
    default TimelinePlan planTimeline(
            List<Trivia> trivias,
            Map<Trivia, List<TriviaOpcion>> opcionesMap,
            VideoFormat format,
            boolean withBgm,
            boolean withTts,
            String voice) {
        return planTimeline(trivias, opcionesMap, format, withBgm, withTts, voice, null, null);
    }

    /**
     * Construye el plan de tiempos para la lista de trivias dada con soporte de locución TTS
     * e introducción temática al inicio del video.
     *
     * @param trivias Lista ordenada de trivias que conformarán el video.
     * @param opcionesMap Mapa de opciones de respuesta por cada trivia.
     * @param format Formato de video deseado.
     * @param withBgm Indica si se debe incluir música de fondo.
     * @param withTts Indica si se activa la síntesis de voz TTS.
     * @param voice Nombre de la voz neuronal deseada.
     * @param introText Texto de introducción a narrar y mostrar (null si no hay intro).
     * @param introTopic Tema o subtema correspondiente a la trivia.
     * @return Objeto TimelinePlan con los tiempos y rutas de audio resueltos.
     */
    TimelinePlan planTimeline(
            List<Trivia> trivias,
            Map<Trivia, List<TriviaOpcion>> opcionesMap,
            VideoFormat format,
            boolean withBgm,
            boolean withTts,
            String voice,
            String introText,
            String introTopic
    );
}
