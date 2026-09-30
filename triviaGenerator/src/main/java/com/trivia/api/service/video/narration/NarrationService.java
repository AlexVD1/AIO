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
     * Construye el plan de tiempos para la lista de trivias dada (sin TTS).
     */
    default TimelinePlan planTimeline(List<Trivia> trivias, VideoFormat format, boolean withBgm) {
        return planTimeline(trivias, Map.of(), format, withBgm, false, "es-MX-JorgeNeural");
    }

    /**
     * Construye el plan de tiempos para la lista de trivias dada con soporte opcional de locución TTS.
     *
     * @param trivias Lista ordenada de trivias que conformarán el video.
     * @param opcionesMap Mapa de opciones de respuesta por cada trivia.
     * @param format Formato de video deseado.
     * @param withBgm Indica si se debe incluir música de fondo.
     * @param withTts Indica si se activa la síntesis de voz TTS.
     * @param voice Nombre de la voz neuronal deseada.
     * @return Objeto TimelinePlan con los tiempos y rutas de audio resueltos.
     */
    TimelinePlan planTimeline(
            List<Trivia> trivias,
            Map<Trivia, List<TriviaOpcion>> opcionesMap,
            VideoFormat format,
            boolean withBgm,
            boolean withTts,
            String voice
    );
}
