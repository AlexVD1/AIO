package com.kidsanim.api.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final Map<String, String> textCache = new ConcurrentHashMap<>();
    private Map<String, Object> scriptSchemaCache;
    private Map<String, Object> reviewSchemaCache;

    public PromptTemplateService(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public String getSystemPrompt() {
        return loadResourceAsString("classpath:prompts/system_prompt_kids.txt");
    }

    public String getReviewerPrompt() {
        return loadResourceAsString("classpath:prompts/pedagogical_reviewer_prompt.txt");
    }

    public String getTopicPrompt(EducationalTopicType topicType) {
        String path = "classpath:prompts/topic_" + topicType.name() + ".txt";
        String content = loadResourceAsString(path);
        if (content == null || content.isBlank()) {
            return "Tema educativo: " + topicType.name() + ". Enfocado en aprendizaje preescolar divertido.";
        }
        return content;
    }

    public Map<String, Object> getEpisodeScriptSchema() {
        if (scriptSchemaCache != null) return scriptSchemaCache;
        try {
            Resource res = resourceLoader.getResource("classpath:schemas/EpisodeScript.schema.json");
            try (InputStream in = res.getInputStream()) {
                scriptSchemaCache = objectMapper.readValue(in, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar EpisodeScript.schema.json, usando esquema fallback: {}", e.getMessage());
            scriptSchemaCache = Map.of("type", "object");
        }
        return scriptSchemaCache;
    }

    public Map<String, Object> getPedagogicalReviewSchema() {
        if (reviewSchemaCache != null) return reviewSchemaCache;
        reviewSchemaCache = Map.of(
                "type", "object",
                "properties", Map.of(
                        "approved", Map.of("type", "boolean"),
                        "issues", Map.of("type", "array", "items", Map.of("type", "string"))
                ),
                "required", List.of("approved", "issues")
        );
        return reviewSchemaCache;
    }

    public String buildUserPrompt(Series series,
                                  EducationalTopicType topicType,
                                  String topicDetail,
                                  String learningObjective,
                                  int targetDurationSec) {
        StringBuilder sb = new StringBuilder();
        sb.append("GENERACIÓN DE GUION PARA LA SERIE '").append(series.getName()).append("'\n\n");

        sb.append("--- INFORMACIÓN DE LA SERIE Y BIBLIA ---\n");
        sb.append("- Idioma: ").append(series.getLanguage()).append("\n");
        sb.append("- Rango de edad objetivo: ").append(series.getTargetAgeMin()).append(" a ").append(series.getTargetAgeMax()).append(" años\n");
        sb.append("- Aspect Ratio: ").append(series.getAspectRatio()).append("\n");
        sb.append("- Mood musical predeterminado: ").append(series.getDefaultBgmMood()).append("\n\n");

        sb.append("PERSONAJES DISPONIBLES EN LA SERIE:\n");
        List<Character> characters = series.getCharacters();
        if (characters != null && !characters.isEmpty()) {
            for (Character c : characters) {
                sb.append("• ").append(c.getName()).append(" (Rol: ").append(c.getRole()).append(")\n");
                sb.append("  - Descripción canónica: ").append(c.getCanonicalPrompt()).append("\n");
                if (c.getPersonality() != null && !c.getPersonality().isBlank()) {
                    sb.append("  - Personalidad: ").append(c.getPersonality()).append("\n");
                }
                sb.append("  - Voz TTS: ").append(c.getTtsVoice()).append("\n");
            }
        } else {
            sb.append("• Tito (Anfitrión principal, zorro amigable curioso)\n");
        }
        sb.append("\n");

        sb.append("LOCACIONES DISPONIBLES EN LA SERIE:\n");
        List<Location> locations = series.getLocations();
        if (locations != null && !locations.isEmpty()) {
            for (Location loc : locations) {
                sb.append("• '").append(loc.getName()).append("': ").append(loc.getPrompt()).append("\n");
            }
        } else {
            sb.append("• 'Huerto Soleado': bosque con manzanos alegres\n");
        }
        sb.append("\n");

        sb.append("--- TEMA Y OBJETIVO PEDAGÓGICO ---\n");
        sb.append("- Tipo de Tema: ").append(topicType.name()).append("\n");
        sb.append("- Detalle específico del tema: ").append(topicDetail).append("\n");
        if (learningObjective != null && !learningObjective.isBlank()) {
            sb.append("- Objetivo didáctico: ").append(learningObjective).append("\n");
        }
        sb.append("- Duración objetivo total: ").append(targetDurationSec).append(" segundos\n\n");

        sb.append("--- INSTRUCCIONES ESPECÍFICAS DEL TEMA ---\n");
        sb.append(getTopicPrompt(topicType)).append("\n\n");

        sb.append("--- REGLAS ESTRUCTURALES ESTRICTAS (CUMPLIMIENTO OBLIGATORIO) ---\n");
        sb.append("1. NÚMERO DE ESCENAS: Genera entre 4 y 6 escenas pedagógicas (INTRO, CONCEPT, EXAMPLE, CHALLENGE, RECAP/OUTRO).\n");
        sb.append("2. TOTAL DE PLANOS (CRÍTICO): El episodio DEBE contener un TOTAL de entre 12 y 16 planos en total (cada escena debe contener al menos 2 o 3 planos 'shots').\n");
        sb.append("3. VISUAL PROMPT OBLIGATORIO: CADA plano (shot) DEBE incluir 'visualPrompt' descriptivo en inglés para generación de imágenes (mínimo 15 palabras describiendo la toma, personajes y entorno).\n");
        sb.append("4. NARRACIÓN: Cada plano debe tener entre 4 y 14 palabras claras y amables en español.\n");
        sb.append("5. OVERLAYS PEDAGÓGICOS: En temas de tipo COUNTING, los overlays permitidos son únicamente 'BIG_NUMBER' y 'COUNTER'. Los efectos de sonido ('pop', 'chime', etc.) van en el campo 'sfx' del shot, NO como tipo de overlay.\n");
        sb.append("6. BIBLIA: Utiliza exclusivamente los personajes y locaciones definidos en la biblia de la serie.\n");

        return sb.toString();
    }

    private String loadResourceAsString(String location) {
        return textCache.computeIfAbsent(location, loc -> {
            try {
                Resource resource = resourceLoader.getResource(loc);
                if (!resource.exists()) return "";
                try (InputStream is = resource.getInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                }
            } catch (Exception e) {
                log.warn("No se pudo leer recurso {}: {}", loc, e.getMessage());
                return "";
            }
        });
    }
}
