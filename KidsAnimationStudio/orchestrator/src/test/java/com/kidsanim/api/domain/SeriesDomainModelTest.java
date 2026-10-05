package com.kidsanim.api.domain;

import com.kidsanim.api.domain.enums.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SeriesDomainModelTest {

    @Test
    void styleProfileLifecycleAndDefaults() {
        StyleProfile profile = new StyleProfile("Pixar 3D", "DreamShaper.safetensors", "3d style", "bad quality");
        profile.prePersist();

        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getUpdatedAt()).isNotNull();
        assertThat(profile.getVideoModel()).isEqualTo(VideoModel.LTX);
        assertThat(profile.getSampler()).isEqualTo("dpmpp_sde");

        OffsetDateTime firstUpdate = profile.getUpdatedAt();
        profile.preUpdate();
        assertThat(profile.getUpdatedAt()).isAfterOrEqualTo(firstUpdate);
    }

    @Test
    void seriesRelationshipsAndLifecycle() {
        StyleProfile profile = new StyleProfile("Pixar 3D", "DreamShaper.safetensors", "3d style", "bad quality");
        Series series = new Series("Tito el zorrito", "Aventuras de Tito", profile);
        series.prePersist();

        assertThat(series.getCreatedAt()).isNotNull();
        assertThat(series.getLanguage()).isEqualTo("es-MX");
        assertThat(series.getStyleProfile()).isEqualTo(profile);

        Character character = new Character(series, "Tito", CharacterRole.HOST, "a cute fox");
        series.getCharacters().add(character);
        assertThat(series.getCharacters()).hasSize(1);

        Location loc = new Location(series, "Huerto", "apple orchard");
        series.getLocations().add(loc);
        assertThat(series.getLocations()).hasSize(1);
    }

    @Test
    void characterLifecycleAndDefaults() {
        Series series = new Series();
        Character c = new Character(series, "Tito", CharacterRole.HOST, "a cute red fox");
        c.prePersist();

        assertThat(c.getStatus()).isEqualTo(CharacterStatus.DRAFT);
        assertThat(c.getTtsVoice()).isEqualTo("ef_dora");
        assertThat(c.getIpadapterWeight()).isEqualTo(new BigDecimal("0.85"));
        assertThat(c.getCreatedAt()).isNotNull();
    }

    @Test
    void episodeAndSceneAndShotHierarchy() {
        Series series = new Series();
        series.setId(UUID.randomUUID());

        BatchJob batchJob = new BatchJob("Lote 1", series, 5);
        batchJob.prePersist();
        assertThat(batchJob.getStatus()).isEqualTo(BatchJobStatus.PENDING);

        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar manzanas", "Numeros 1 al 5", "Episodio 1");
        episode.setBatchJob(batchJob);
        episode.prePersist();
        assertThat(episode.getStatus()).isEqualTo(EpisodeStatus.DRAFT);

        Location location = new Location(series, "Huerto", "apple orchard");
        Scene scene = new Scene(episode, 1, location, ScenePurpose.INTRO, "playful");
        scene.prePersist();
        episode.getScenes().add(scene);

        Character character = new Character(series, "Tito", CharacterRole.HOST, "a cute fox");
        Shot shot = new Shot(scene, 1, "¡Hola amiguitos!", "Tito saludando");
        shot.setCharacter(character);
        shot.setCameraMotion(CameraMotion.SLOW_ZOOM_IN);
        shot.setContinuityMode(ContinuityMode.NEW_KEYFRAME);
        shot.prePersist();
        scene.getShots().add(shot);

        assertThat(episode.getScenes()).hasSize(1);
        assertThat(scene.getShots()).hasSize(1);
        assertThat(shot.getStatus()).isEqualTo(ShotStatus.PENDING);
        assertThat(shot.getSpeaker()).isEqualTo("NARRATOR");

        Asset asset = new Asset(episode, shot, AssetType.KEYFRAME, "/storage/frame_1.png");
        asset.prePersist();
        assertThat(asset.getType()).isEqualTo(AssetType.KEYFRAME);

        PipelineJob job = new PipelineJob(episode, PipelineStage.PLANNING);
        job.prePersist();
        assertThat(job.getStatus()).isEqualTo(PipelineJobStatus.PENDING);
    }
}
