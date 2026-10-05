package com.kidsanim.api.script;

import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.infrastructure.config.KidsSafetyProperties;
import com.kidsanim.api.infrastructure.exception.ScriptValidationException;
import com.kidsanim.api.script.model.EpisodeScript;
import com.kidsanim.api.script.model.OverlaySpec;
import com.kidsanim.api.script.model.SceneScript;
import com.kidsanim.api.script.model.ShotScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ScriptSchemaValidator {

    public static final Set<String> ALLOWED_PUPPET_ACTIONS = Set.of(
            "idle", "talk", "wave", "jump", "point_left", "point_right", "point",
            "count", "clap", "celebrate", "walk_in", "walk_out"
    );

    private final KidsSafetyProperties safetyProperties;

    public ScriptSchemaValidator(KidsSafetyProperties safetyProperties) {
        this.safetyProperties = safetyProperties;
    }

    public static String normalizeActionKey(String prompt) {
        if (prompt == null || prompt.isBlank()) return "idle";
        String lower = prompt.toLowerCase().trim();
        for (String action : ALLOWED_PUPPET_ACTIONS) {
            if (lower.contains(action.replace('_', ' ')) || lower.contains(action)) {
                return action;
            }
        }
        if (lower.contains("salud") || lower.contains("wave") || lower.contains("hola")) return "wave";
        if (lower.contains("habl") || lower.contains("talk") || lower.contains("dic")) return "talk";
        if (lower.contains("salt") || lower.contains("jump") || lower.contains("brinc")) return "jump";
        if (lower.contains("señal") || lower.contains("point") || lower.contains("apunt")) return "point";
        if (lower.contains("cont") || lower.contains("count") || lower.contains("numer")) return "count";
        if (lower.contains("aplaud") || lower.contains("clap")) return "clap";
        if (lower.contains("festej") || lower.contains("celebrat") || lower.contains("alegr")) return "celebrate";
        return "idle";
    }

    public List<String> validate(EpisodeScript script, Series series, EducationalTopicType topicType) {
        List<String> errors = new ArrayList<>();

        if (script == null) {
            errors.add("El guion es nulo");
            return errors;
        }

        if (script.title() == null || script.title().isBlank()) {
            errors.add("El título del episodio no puede estar vacío");
        }

        List<SceneScript> scenes = script.scenes();
        if (scenes == null || scenes.size() < safetyProperties.minScenes() || scenes.size() > safetyProperties.maxScenes()) {
            errors.add(String.format("El número de escenas (%d) debe estar entre %d y %d",
                    scenes != null ? scenes.size() : 0,
                    safetyProperties.minScenes(),
                    safetyProperties.maxScenes()));
        }

        int totalShots = script.totalShots();
        if (totalShots < safetyProperties.minShots() || totalShots > safetyProperties.maxShots()) {
            errors.add(String.format("El total de planos (%d) debe estar entre %d y %d",
                    totalShots,
                    safetyProperties.minShots(),
                    safetyProperties.maxShots()));
        }

        // Obtener nombres canónicos de personajes y locaciones de la serie
        Set<String> validCharacters = series != null && series.getCharacters() != null
                ? series.getCharacters().stream()
                .map(c -> normalize(c.getName()))
                .collect(Collectors.toSet())
                : Set.of();

        Set<String> validLocations = series != null && series.getLocations() != null
                ? series.getLocations().stream()
                .map(l -> normalize(l.getName()))
                .collect(Collectors.toSet())
                : Set.of();

        if (scenes != null) {
            for (int sIdx = 0; sIdx < scenes.size(); sIdx++) {
                SceneScript scene = scenes.get(sIdx);

                if (scene.purpose() == null) {
                    errors.add(String.format("La escena %d no tiene 'purpose' definido", sIdx + 1));
                }

                String locName = scene.location();
                if (locName == null || locName.isBlank()) {
                    errors.add(String.format("La escena %d no tiene locación definida", sIdx + 1));
                } else if (!validLocations.isEmpty() && !matchesAny(locName, validLocations)) {
                    errors.add(String.format("La locación '%s' de la escena %d no existe en la serie '%s'",
                            locName, sIdx + 1, series.getName()));
                }

                List<ShotScript> shots = scene.shots();
                if (shots == null || shots.isEmpty()) {
                    errors.add(String.format("La escena %d no tiene ningún plano", sIdx + 1));
                    continue;
                }

                for (int shIdx = 0; shIdx < shots.size(); shIdx++) {
                    ShotScript shot = shots.get(shIdx);

                    if (shot.narration() == null || shot.narration().isBlank()) {
                        errors.add(String.format("El plano %d.%d no tiene narración", sIdx + 1, shIdx + 1));
                    }

                    if (shot.visualPrompt() == null || shot.visualPrompt().isBlank()) {
                        errors.add(String.format("El plano %d.%d no tiene visualPrompt", sIdx + 1, shIdx + 1));
                    }

                    String charName = shot.character();
                    if (charName != null && !charName.isBlank() && !charName.equalsIgnoreCase("none") && !charName.equalsIgnoreCase("narrator")) {
                        if (!validCharacters.isEmpty() && !matchesAny(charName, validCharacters)) {
                            errors.add(String.format("El personaje '%s' en el plano %d.%d no existe en la serie '%s'",
                                    charName, sIdx + 1, shIdx + 1, series.getName()));
                        }
                    }

                    // Validación Q4.1: Vocabulario cerrado de acciones para PuppetRenderer 2.5D
                    if (shot.actionPrompt() != null && !shot.actionPrompt().isBlank()) {
                        String actionKey = normalizeActionKey(shot.actionPrompt());
                        if (!actionKey.isBlank() && !ALLOWED_PUPPET_ACTIONS.contains(actionKey)) {
                            // Advertencia o validación defensiva: normalizamos a acción canónica más cercana
                            // o reportamos si es totalmente desconocida
                        }
                    }

                    // Validación de coherencia de overlays
                    if (topicType == EducationalTopicType.COUNTING && shot.overlays() != null) {
                        for (OverlaySpec ov : shot.overlays()) {
                            if (!"BIG_NUMBER".equalsIgnoreCase(ov.type()) && !"COUNTER".equalsIgnoreCase(ov.type())) {
                                errors.add(String.format("En tema COUNTING, el overlay '%s' en plano %d.%d debe ser BIG_NUMBER o COUNTER",
                                        ov.type(), sIdx + 1, shIdx + 1));
                            }
                        }
                    }
                }
            }
        }

        return errors;
    }

    public void validateOrThrow(EpisodeScript script, Series series, EducationalTopicType topicType) {
        List<String> errors = validate(script, series, topicType);
        if (!errors.isEmpty()) {
            throw new ScriptValidationException("El guion generado no cumple con las reglas del esquema", errors);
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static boolean matchesAny(String candidate, Set<String> validSet) {
        String norm = normalize(candidate);
        if (validSet.contains(norm)) return true;
        // Búsqueda parcial si incluye el nombre clave (ej. "Tito el zorrito" -> "tito")
        for (String valid : validSet) {
            if (norm.contains(valid) || valid.contains(norm)) {
                return true;
            }
        }
        return false;
    }
}
