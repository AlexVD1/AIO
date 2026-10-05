package com.kidsanim.api.script;

import com.kidsanim.api.domain.Character;
import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.CameraMotion;
import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.ContinuityMode;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.ScenePurpose;
import com.kidsanim.api.infrastructure.config.KidsSafetyProperties;
import com.kidsanim.api.script.model.EpisodeScript;
import com.kidsanim.api.script.model.OverlaySpec;
import com.kidsanim.api.script.model.SceneScript;
import com.kidsanim.api.script.model.ShotScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptSchemaValidatorTest {

    private ScriptSchemaValidator validator;
    private Series testSeries;

    @BeforeEach
    void setUp() {
        KidsSafetyProperties safetyProperties = new KidsSafetyProperties(
                "classpath:content-safety-blocklist.txt",
                14,
                4,
                8,
                12,
                40,
                2
        );
        validator = new ScriptSchemaValidator(safetyProperties);

        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        testSeries = new Series("Tito el zorrito", "Serie educativa para preescolar", styleProfile);
        Character tito = new Character(testSeries, "Tito", CharacterRole.HOST, "cute red fox");
        Location huerto = new Location(testSeries, "Huerto", "huerto de manzanos soleado");

        testSeries.getCharacters().add(tito);
        testSeries.getLocations().add(huerto);
    }

    private EpisodeScript createValidScript(int sceneCount, int shotsPerScene) {
        List<SceneScript> scenes = new ArrayList<>();
        for (int i = 1; i <= sceneCount; i++) {
            List<ShotScript> shots = new ArrayList<>();
            for (int j = 1; j <= shotsPerScene; j++) {
                shots.add(new ShotScript(
                        "tito",
                        "Tito",
                        "¡Miren la manzana número " + j + "!",
                        "Tito pointing to apple",
                        "waving tail",
                        CameraMotion.STATIC,
                        ContinuityMode.NEW_KEYFRAME,
                        List.of(new OverlaySpec("BIG_NUMBER", String.valueOf(j), "TOP_RIGHT", null)),
                        List.of("pop"),
                        300
                ));
            }
            scenes.add(new SceneScript(ScenePurpose.INTRO, "Huerto", "cheerful", shots));
        }
        return new EpisodeScript("Contamos con Tito", "Contar del 1 al 5", scenes);
    }

    @Test
    void acceptsValidScript() {
        // 4 scenes, 3 shots per scene = 12 shots
        EpisodeScript script = createValidScript(4, 3);
        List<String> errors = validator.validate(script, testSeries, EducationalTopicType.COUNTING);
        assertThat(errors).isEmpty();
    }

    @Test
    void rejectsScriptWithTooFewScenes() {
        // 2 scenes is below minScenes (4)
        EpisodeScript script = createValidScript(2, 6);
        List<String> errors = validator.validate(script, testSeries, EducationalTopicType.COUNTING);

        assertThat(errors).anyMatch(e -> e.contains("número de escenas (2) debe estar entre 4 y 8"));
    }

    @Test
    void rejectsScriptWithTooFewShots() {
        // 4 scenes, 2 shots per scene = 8 shots (below minShots 12)
        EpisodeScript script = createValidScript(4, 2);
        List<String> errors = validator.validate(script, testSeries, EducationalTopicType.COUNTING);

        assertThat(errors).anyMatch(e -> e.contains("total de planos (8) debe estar entre 12 y 40"));
    }

    @Test
    void rejectsScriptWithInvalidCharacterName() {
        EpisodeScript script = createValidScript(4, 3);
        // Replace character in one shot with unknown character
        ShotScript invalidShot = new ShotScript(
                "dragón",
                "DragonRojo",
                "¡Hola!",
                "Dragon breathing fire",
                "fly",
                CameraMotion.STATIC,
                ContinuityMode.NEW_KEYFRAME,
                List.of(),
                List.of(),
                300
        );
        script.scenes().get(0).shots().set(0, invalidShot);

        List<String> errors = validator.validate(script, testSeries, EducationalTopicType.COUNTING);

        assertThat(errors).anyMatch(e -> e.contains("personaje 'DragonRojo'") && e.contains("no existe en la serie"));
    }

    @Test
    void rejectsScriptWithInvalidLocationName() {
        EpisodeScript script = createValidScript(4, 3);
        // Change first scene's location to something not in series bible
        SceneScript invalidScene = new SceneScript(ScenePurpose.INTRO, "CastilloTenebroso", "mysterious", script.scenes().get(0).shots());
        script.scenes().set(0, invalidScene);

        List<String> errors = validator.validate(script, testSeries, EducationalTopicType.COUNTING);

        assertThat(errors).anyMatch(e -> e.contains("locación 'CastilloTenebroso'") && e.contains("no existe en la serie"));
    }

    @Test
    void rejectsIncompatibleOverlayTypeForCounting() {
        EpisodeScript script = createValidScript(4, 3);
        // Add bad overlay type
        ShotScript shotWithBadOverlay = new ShotScript(
                "tito",
                "Tito",
                "¡Miren esto!",
                "Tito looking",
                "waving",
                CameraMotion.STATIC,
                ContinuityMode.NEW_KEYFRAME,
                List.of(new OverlaySpec("CUSTOM_STICKER", "star", "CENTER", null)),
                List.of(),
                300
        );
        script.scenes().get(0).shots().set(0, shotWithBadOverlay);

        List<String> errors = validator.validate(script, testSeries, EducationalTopicType.COUNTING);

        assertThat(errors).anyMatch(e -> e.contains("overlay 'CUSTOM_STICKER'") && e.contains("debe ser BIG_NUMBER o COUNTER"));
    }
}
