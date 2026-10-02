package com.trivia.api.service.video.narration;

import com.trivia.api.domain.Trivia;
import com.trivia.api.domain.TriviaOpcion;
import com.trivia.api.service.video.TimelinePlan;
import com.trivia.api.service.video.TriviaSceneTiming;
import com.trivia.api.service.video.VideoFormat;
import com.trivia.api.service.video.VideoIntroTiming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementación de NarrationService para la fase estándar (sin TTS).
 *
 * Asigna duraciones estándar fijas calibradas para dar suficiente tiempo
 * de lectura visual al usuario antes de revelar la respuesta.
 */
@Service("noOpNarrationService")
public class NoOpNarrationService implements NarrationService {

    private static final Logger log = LoggerFactory.getLogger(NoOpNarrationService.class);

    private final double questionDuration;
    private final double countdownDuration;
    private final double answerDuration;

    public NoOpNarrationService(
            @Value("${trivia.video.timing.question:7.0}") double questionDuration,
            @Value("${trivia.video.timing.countdown:5.0}") double countdownDuration,
            @Value("${trivia.video.timing.answer:6.5}") double answerDuration) {
        this.questionDuration = questionDuration;
        this.countdownDuration = countdownDuration;
        this.answerDuration = answerDuration;
    }

    @Override
    public TimelinePlan planTimeline(List<Trivia> trivias, VideoFormat format, boolean withBgm) {
        return planTimeline(trivias, Map.of(), format, withBgm, false, null, null, null);
    }

    @Override
    public TimelinePlan planTimeline(
            List<Trivia> trivias,
            Map<Trivia, List<TriviaOpcion>> opcionesMap,
            VideoFormat format,
            boolean withBgm,
            boolean withTts,
            String voice) {
        return planTimeline(trivias, opcionesMap, format, withBgm, withTts, voice, null, null);
    }

    @Override
    public TimelinePlan planTimeline(
            List<Trivia> trivias,
            Map<Trivia, List<TriviaOpcion>> opcionesMap,
            VideoFormat format,
            boolean withBgm,
            boolean withTts,
            String voice,
            String introText,
            String introTopic) {

        log.debug("Planificando timeline sin TTS para {} trivias (formato: {})", trivias.size(), format);
        List<TriviaSceneTiming> scenes = new ArrayList<>();
        int total = trivias.size();

        for (int i = 0; i < total; i++) {
            Trivia trivia = trivias.get(i);
            int index = i + 1;
            String headerText = String.format("PREGUNTA %d DE %d", index, total);

            scenes.add(new TriviaSceneTiming(
                    trivia.getId(),
                    index,
                    total,
                    headerText,
                    questionDuration,
                    countdownDuration,
                    answerDuration,
                    null,
                    null
            ));
        }

        VideoIntroTiming introTiming = null;
        if (introText != null && !introText.isBlank()) {
            double introDuration = 4.0;
            introTiming = new VideoIntroTiming(introText, introTopic, introDuration, null);
        }

        return new TimelinePlan(introTiming, scenes, format, withBgm);
    }
}
