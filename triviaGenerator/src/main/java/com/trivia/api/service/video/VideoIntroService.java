package com.trivia.api.service.video;

import com.trivia.api.ai.TriviaAiClient;
import com.trivia.api.domain.Trivia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio encargado de gestionar el catálogo de introducciones, la sanitización de textos,
 * la deducción del tema/subtema y la generación dinámica de frases de inicio para videos.
 */
@Service
public class VideoIntroService {

    private static final Logger log = LoggerFactory.getLogger(VideoIntroService.class);

    private static final int MAX_INTRO_LENGTH = 160;
    private static final String DEFAULT_TOPIC = "cultura general";

    private final TriviaAiClient aiClient;
    private final List<VideoIntroTemplate> catalog;

    public VideoIntroService(TriviaAiClient aiClient) {
        this.aiClient = aiClient;
        this.catalog = initCatalog();
    }

    private List<VideoIntroTemplate> initCatalog() {
        return List.of(
                new VideoIntroTemplate("tpl_pon_a_prueba", "Pon a prueba tus conocimientos sobre {tema}.", "Pon a prueba tus conocimientos sobre Historia romana."),
                new VideoIntroTemplate("tpl_que_tanto_sabes", "¿Qué tanto sabes de {tema}?", "¿Qué tanto sabes de Interstellar?"),
                new VideoIntroTemplate("tpl_cuanto_sabes", "¿Cuánto sabes sobre {tema}?", "¿Cuánto sabes sobre el mundo de Harry Potter?"),
                new VideoIntroTemplate("tpl_demuestra", "Demuestra cuánto sabes sobre {tema}.", "Demuestra cuánto sabes sobre la geografía de México."),
                new VideoIntroTemplate("tpl_experto", "¿Te consideras experto en {tema}? ¡Vamos a comprobarlo!", "¿Te consideras experto en Física? ¡Vamos a comprobarlo!"),
                new VideoIntroTemplate("tpl_solo_los_que_saben", "Solo los que saben de {tema} podrán responderlas todas.", "Solo los que saben de Astronomía podrán responderlas todas."),
                new VideoIntroTemplate("tpl_puedes_responder", "¿Puedes responder estas preguntas sobre {tema}?", "¿Puedes responder estas preguntas sobre Cine clásico?")
        );
    }

    /**
     * Retorna el catálogo completo de plantillas reutilizables disponibles.
     */
    public List<VideoIntroTemplate> getCatalog() {
        return catalog;
    }

    /**
     * Deduce el tema o subtema de la trivia a partir de un tema explícito o del lote de trivias.
     */
    public String resolveTopic(String explicitTopic, List<Trivia> trivias) {
        if (explicitTopic != null && !explicitTopic.isBlank()) {
            return sanitizeTopic(explicitTopic);
        }

        if (trivias != null && !trivias.isEmpty()) {
            // 1. Buscar el primer subtema no vacío
            for (Trivia t : trivias) {
                if (t.getSubtema() != null && !t.getSubtema().isBlank()) {
                    return sanitizeTopic(t.getSubtema());
                }
            }
            // 2. Si no hay subtema, usar el nombre de la categoría/tipo
            for (Trivia t : trivias) {
                if (t.getTipoTrivia() != null && t.getTipoTrivia().getNombre() != null && !t.getTipoTrivia().getNombre().isBlank()) {
                    return sanitizeTopic(t.getTipoTrivia().getNombre());
                }
            }
        }

        return DEFAULT_TOPIC;
    }

    /**
     * Resuelve el texto final de introducción aplicando la modalidad seleccionada.
     */
    public String resolveIntroText(
            VideoIntroMode mode,
            String explicitTopic,
            String templateOrId,
            String customText,
            List<Trivia> trivias,
            String idioma) {

        if (mode == null || mode == VideoIntroMode.NONE) {
            return null;
        }

        String topic = resolveTopic(explicitTopic, trivias);

        switch (mode) {
            case TEMPLATE -> {
                String pattern = findTemplatePattern(templateOrId);
                String result = pattern.replace("{tema}", topic).replace("{TEMA}", topic.toUpperCase());
                return sanitizeIntroText(result);
            }
            case CUSTOM -> {
                if (customText == null || customText.isBlank()) {
                    log.warn("Modalidad CUSTOM seleccionada pero customText está vacío. Usando plantilla por defecto.");
                    return sanitizeIntroText(catalog.get(0).template().replace("{tema}", topic));
                }
                return sanitizeIntroText(customText);
            }
            case AI -> {
                // Si el usuario ya suministró un texto generado en preview, reutilizarlo
                if (customText != null && !customText.isBlank()) {
                    return sanitizeIntroText(customText);
                }
                try {
                    log.info("Generando frase de introducción por IA para tema '{}'", topic);
                    String aiText = aiClient.generateVideoIntro(topic, idioma != null ? idioma : "es-MX");
                    if (aiText != null && !aiText.isBlank()) {
                        return sanitizeIntroText(aiText);
                    }
                } catch (Exception e) {
                    log.warn("Fallo al generar introducción por IA ({}); recurriendo a plantilla de respaldo", e.getMessage());
                }
                // Fallback seguro a plantilla
                return sanitizeIntroText(catalog.get(1).template().replace("{tema}", topic));
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Busca la plantilla por ID o devuelve la cadena si ya es una plantilla con {tema},
     * o devuelve la plantilla predeterminada si no se encuentra.
     */
    public String findTemplatePattern(String templateOrId) {
        if (templateOrId != null && !templateOrId.isBlank()) {
            // Buscar por ID
            for (VideoIntroTemplate tpl : catalog) {
                if (tpl.id().equalsIgnoreCase(templateOrId.trim())) {
                    return tpl.template();
                }
            }
            // Si contiene el marcador {tema}, se usa directamente
            if (templateOrId.contains("{tema}") || templateOrId.contains("{TEMA}")) {
                return templateOrId.trim();
            }
        }
        return catalog.get(0).template();
    }

    /**
     * Limpia y sanitiza el tema de la trivia.
     */
    public String sanitizeTopic(String topic) {
        if (topic == null) return DEFAULT_TOPIC;
        String cleaned = topic.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (cleaned.length() > 60) {
            cleaned = cleaned.substring(0, 60).trim();
        }
        return cleaned.isEmpty() ? DEFAULT_TOPIC : cleaned;
    }

    /**
     * Sanitiza el texto de introducción para garantizar que sea seguro para FFmpeg,
     * no desborde la pantalla y tenga buena legibilidad.
     */
    public String sanitizeIntroText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Normalizar saltos de línea y espacios redundantes
        String cleaned = text.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        // Quitar comillas envolventes si las tiene
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("«") && cleaned.endsWith("»")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        // Limitar longitud máxima para legibilidad visual en pantalla
        if (cleaned.length() > MAX_INTRO_LENGTH) {
            cleaned = cleaned.substring(0, MAX_INTRO_LENGTH - 3).trim() + "...";
        }

        return cleaned;
    }
}
