package com.kidsanim.api.controller;

import com.kidsanim.api.domain.Episode;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.EducationalTopicType;
import com.kidsanim.api.domain.enums.EpisodeStatus;
import com.kidsanim.api.dto.EpisodeDetailResponse;
import com.kidsanim.api.script.EpisodeScriptService;
import com.kidsanim.api.script.model.EpisodeScript;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EpisodeController.class)
class EpisodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EpisodeScriptService episodeScriptService;

    @MockBean
    private com.kidsanim.api.service.NarrationStageService narrationStageService;

    @MockBean
    private com.kidsanim.api.service.KeyframeStageService keyframeStageService;

    @MockBean
    private com.kidsanim.api.service.AnimationStageService animationStageService;

    @MockBean
    private com.kidsanim.api.service.RenderStageService renderStageService;

    @MockBean
    private com.kidsanim.api.repository.EpisodeRepository episodeRepository;

    @MockBean
    private com.kidsanim.api.pipeline.EpisodePipelineExecutor episodePipelineExecutor;

    @MockBean
    private com.kidsanim.api.export.service.ExportService exportService;

    @Test
    void createEpisodeReturns201Created() throws Exception {
        UUID seriesId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "Aprender números", "¡Contamos con Tito!");
        episode.setStatus(EpisodeStatus.NARRATION);
        episode.setFingerprint("fp_sample_123");

        when(episodeScriptService.generateAndSaveEpisode(eq(seriesId), any())).thenReturn(episode);

        mockMvc.perform(post("/api/v1/series/{seriesId}/episodes", seriesId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicType": "COUNTING",
                                  "topicDetail": "Contar del 1 al 5",
                                  "learningObjective": "Aprender números",
                                  "targetDurationSec": 120,
                                  "autoApprove": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("¡Contamos con Tito!"))
                .andExpect(jsonPath("$.topicType").value("COUNTING"))
                .andExpect(jsonPath("$.status").value("NARRATION"))
                .andExpect(jsonPath("$.fingerprint").value("fp_sample_123"));
    }

    @Test
    void getEpisodesBySeriesReturnsList() throws Exception {
        UUID seriesId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode ep1 = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "Aprender", "Episodio 1");
        ep1.setStatus(EpisodeStatus.AWAITING_SCRIPT_APPROVAL);

        when(episodeScriptService.findBySeriesId(seriesId)).thenReturn(List.of(ep1));

        mockMvc.perform(get("/api/v1/series/{seriesId}/episodes", seriesId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Episodio 1"))
                .andExpect(jsonPath("$[0].status").value("AWAITING_SCRIPT_APPROVAL"));
    }

    @Test
    void getEpisodeDetailReturnsDetailResponse() throws Exception {
        UUID episodeId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        EpisodeScript script = new EpisodeScript("Episodio 1", "Aprender", List.of());
        EpisodeDetailResponse detail = new EpisodeDetailResponse(
                episodeId,
                seriesId,
                EducationalTopicType.COUNTING,
                "Contar",
                "Aprender",
                "Episodio 1",
                EpisodeStatus.NARRATION,
                "fp_123",
                null,
                script,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(episodeScriptService.getDetail(episodeId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/episodes/{id}", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(episodeId.toString()))
                .andExpect(jsonPath("$.title").value("Episodio 1"))
                .andExpect(jsonPath("$.script.title").value("Episodio 1"));
    }

    @Test
    void getEpisodeScriptReturnsScript() throws Exception {
        UUID episodeId = UUID.randomUUID();
        EpisodeScript script = new EpisodeScript("Episodio 1", "Aprender números", List.of());

        when(episodeScriptService.getScript(episodeId)).thenReturn(script);

        mockMvc.perform(get("/api/v1/episodes/{id}/script", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episodio 1"))
                .andExpect(jsonPath("$.learningObjective").value("Aprender números"));
    }

    @Test
    void approveScriptReturnsUpdatedEpisode() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Aprender", "Episodio 1");
        episode.setStatus(EpisodeStatus.NARRATION);

        when(episodeScriptService.approveScript(episodeId)).thenReturn(episode);

        mockMvc.perform(post("/api/v1/episodes/{id}/approve-script", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episodio 1"))
                .andExpect(jsonPath("$.status").value("NARRATION"));
    }

    @Test
    void runNarrationReturnsUpdatedEpisode() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Aprender", "Episodio 1");
        episode.setStatus(EpisodeStatus.KEYFRAMES);

        when(narrationStageService.processEpisodeNarration(episodeId)).thenReturn(episode);

        mockMvc.perform(post("/api/v1/episodes/{id}/narration", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episodio 1"))
                .andExpect(jsonPath("$.status").value("KEYFRAMES"));
    }

    @Test
    void getShotNarrationReturnsNarrationDetails() throws Exception {
        UUID shotId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.kidsanim.api.dto.ShotNarrationResponse response = new com.kidsanim.api.dto.ShotNarrationResponse(
                shotId,
                sceneId,
                1,
                "Tito",
                "em_alex",
                "¡Hola!",
                2500,
                3000,
                "/storage/audio/shot_1_1.wav",
                List.of(new com.kidsanim.api.gateway.AiGatewayClient.WordTimestamp("Hola", 0, 500))
        );

        when(narrationStageService.getShotNarration(shotId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shots/{shotId}/narration", shotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shotId").value(shotId.toString()))
                .andExpect(jsonPath("$.voice").value("em_alex"))
                .andExpect(jsonPath("$.audioDurationMs").value(2500))
                .andExpect(jsonPath("$.targetDurationMs").value(3000))
                .andExpect(jsonPath("$.wordTimestamps.length()").value(1));
    }

    @Test
    void regenerateShotNarrationReturnsUpdatedDetails() throws Exception {
        UUID shotId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.kidsanim.api.dto.ShotNarrationResponse response = new com.kidsanim.api.dto.ShotNarrationResponse(
                shotId,
                sceneId,
                1,
                "Tito",
                "em_alex",
                "¡Hola!",
                2500,
                3000,
                "/storage/audio/shot_1_1.wav",
                List.of(new com.kidsanim.api.gateway.AiGatewayClient.WordTimestamp("Hola", 0, 500))
        );

        when(narrationStageService.getShotNarration(shotId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/shots/{shotId}/regenerate-narration", shotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shotId").value(shotId.toString()))
                .andExpect(jsonPath("$.audioDurationMs").value(2500));
    }

    @Test
    void runKeyframesReturnsUpdatedEpisode() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Aprender", "Episodio 1");
        episode.setStatus(EpisodeStatus.ANIMATION);

        when(keyframeStageService.processEpisodeKeyframes(episodeId)).thenReturn(episode);

        mockMvc.perform(post("/api/v1/episodes/{id}/keyframes", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episodio 1"))
                .andExpect(jsonPath("$.status").value("ANIMATION"));
    }

    @Test
    void getShotKeyframeReturnsKeyframeDetails() throws Exception {
        UUID shotId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.kidsanim.api.dto.ShotKeyframeResponse response = new com.kidsanim.api.dto.ShotKeyframeResponse(
                shotId,
                sceneId,
                1,
                com.kidsanim.api.domain.enums.ShotStatus.KEYFRAME_READY,
                com.kidsanim.api.domain.enums.ContinuityMode.NEW_KEYFRAME,
                "Tito",
                "/storage/episodes/ep1/keyframes/shot_1_1.png",
                0.88,
                42L,
                1,
                "cute fox waving"
        );

        when(keyframeStageService.getShotKeyframe(shotId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shots/{shotId}/keyframe", shotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shotId").value(shotId.toString()))
                .andExpect(jsonPath("$.status").value("KEYFRAME_READY"))
                .andExpect(jsonPath("$.keyframePath").value("/storage/episodes/ep1/keyframes/shot_1_1.png"))
                .andExpect(jsonPath("$.similarityScore").value(0.88))
                .andExpect(jsonPath("$.characterName").value("Tito"));
    }

    @Test
    void regenerateShotKeyframeReturnsUpdatedDetails() throws Exception {
        UUID shotId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.kidsanim.api.dto.ShotKeyframeResponse response = new com.kidsanim.api.dto.ShotKeyframeResponse(
                shotId,
                sceneId,
                1,
                com.kidsanim.api.domain.enums.ShotStatus.KEYFRAME_READY,
                com.kidsanim.api.domain.enums.ContinuityMode.NEW_KEYFRAME,
                "Tito",
                "/storage/episodes/ep1/keyframes/shot_1_1_attempt_2.png",
                0.91,
                999L,
                2,
                "cute fox smiling"
        );

        when(keyframeStageService.getShotKeyframe(shotId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/shots/{shotId}/regenerate-keyframe", shotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shotId").value(shotId.toString()))
                .andExpect(jsonPath("$.similarityScore").value(0.91))
                .andExpect(jsonPath("$.attempts").value(2));
    }

    @Test
    void runAnimationReturnsUpdatedEpisode() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Aprender", "Episodio 1");
        episode.setStatus(EpisodeStatus.SUBTITLES_OVERLAYS);

        when(animationStageService.processEpisodeAnimation(episodeId)).thenReturn(episode);

        mockMvc.perform(post("/api/v1/episodes/{id}/animation", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episodio 1"))
                .andExpect(jsonPath("$.status").value("SUBTITLES_OVERLAYS"));
    }

    @Test
    void getShotAnimationReturnsAnimationDetails() throws Exception {
        UUID shotId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.kidsanim.api.dto.ShotAnimationResponse response = new com.kidsanim.api.dto.ShotAnimationResponse(
                shotId,
                sceneId,
                1,
                com.kidsanim.api.domain.enums.ShotStatus.ANIMATED,
                com.kidsanim.api.domain.enums.ContinuityMode.NEW_KEYFRAME,
                "/storage/episodes/ep1/clips/shot_1_1.mp4",
                "/storage/episodes/ep1/clips/shot_1_1_last_frame.png",
                48,
                3000,
                42L,
                1,
                "the fox waves"
        );

        when(animationStageService.getShotAnimation(shotId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shots/{shotId}/animation", shotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shotId").value(shotId.toString()))
                .andExpect(jsonPath("$.status").value("ANIMATED"))
                .andExpect(jsonPath("$.clipPath").value("/storage/episodes/ep1/clips/shot_1_1.mp4"))
                .andExpect(jsonPath("$.lastFramePath").value("/storage/episodes/ep1/clips/shot_1_1_last_frame.png"))
                .andExpect(jsonPath("$.frames").value(48))
                .andExpect(jsonPath("$.durationMs").value(3000));
    }

    @Test
    void regenerateShotAnimationReturnsUpdatedDetails() throws Exception {
        UUID shotId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.kidsanim.api.dto.ShotAnimationResponse response = new com.kidsanim.api.dto.ShotAnimationResponse(
                shotId,
                sceneId,
                1,
                com.kidsanim.api.domain.enums.ShotStatus.ANIMATED,
                com.kidsanim.api.domain.enums.ContinuityMode.NEW_KEYFRAME,
                "/storage/episodes/ep1/clips/shot_1_1_attempt_2.mp4",
                "/storage/episodes/ep1/clips/shot_1_1_attempt_2_last_frame.png",
                64,
                4000,
                999L,
                2,
                "the fox jumps"
        );

        when(animationStageService.getShotAnimation(shotId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/shots/{shotId}/regenerate-animation", shotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shotId").value(shotId.toString()))
                .andExpect(jsonPath("$.frames").value(64))
                .andExpect(jsonPath("$.attempts").value(2));
    }

    @Test
    void runRenderReturnsUpdatedEpisode() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "Aprender", "Episodio Renderizado");
        episode.setStatus(EpisodeStatus.VIDEO_QA);
        episode.setFinalVideoPath("/storage/episodes/" + episodeId + "/video/final.mp4");
        episode.setDurationSeconds(new java.math.BigDecimal("12.50"));

        when(renderStageService.renderEpisode(episodeId)).thenReturn(episode);

        mockMvc.perform(post("/api/v1/episodes/{id}/render", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episodio Renderizado"))
                .andExpect(jsonPath("$.status").value("VIDEO_QA"));
    }

    @Test
    void getEpisodeVideoReturnsDetails() throws Exception {
        UUID episodeId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar del 1 al 5", "Aprender", "Episodio Video");
        episode.setId(episodeId);
        episode.setStatus(EpisodeStatus.VIDEO_QA);
        episode.setFinalVideoPath("/storage/episodes/" + episodeId + "/video/final.mp4");
        episode.setDurationSeconds(new java.math.BigDecimal("15.00"));

        when(episodeRepository.findById(episodeId)).thenReturn(java.util.Optional.of(episode));

        mockMvc.perform(get("/api/v1/episodes/{id}/video", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.episodeId").value(episodeId.toString()))
                .andExpect(jsonPath("$.title").value("Episodio Video"))
                .andExpect(jsonPath("$.status").value("VIDEO_QA"))
                .andExpect(jsonPath("$.durationSeconds").value(15.0));
    }

    @Test
    void startPipelineReturns202Accepted() throws Exception {
        UUID episodeId = UUID.randomUUID();
        UUID trackingId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Cartoon", "dreamshaper.safetensors", "3d cartoon", "bad quality");
        Series series = new Series("Tito el zorrito", "Preescolar", styleProfile);
        Episode episode = new Episode(series, EducationalTopicType.COUNTING, "Contar", "Obj", "Episodio Async");
        episode.setId(episodeId);

        when(episodeRepository.findById(episodeId)).thenReturn(java.util.Optional.of(episode));
        when(episodePipelineExecutor.registerPipeline(eq(episodeId), any())).thenReturn(trackingId);

        com.kidsanim.api.dto.EpisodePipelineStatusResponse statusResponse = new com.kidsanim.api.dto.EpisodePipelineStatusResponse(
                episodeId, trackingId, "Episodio Async", "PLANNING", "PLANNING", 5, "Iniciando...", null, null, java.time.OffsetDateTime.now(), null
        );
        when(episodePipelineExecutor.getStatus(trackingId)).thenReturn(statusResponse);

        mockMvc.perform(post("/api/v1/episodes/{id}/start-pipeline", episodeId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.episodeId").value(episodeId.toString()))
                .andExpect(jsonPath("$.status").value("PLANNING"));
    }

    @Test
    void resumeEpisodeReturnsStatus() throws Exception {
        UUID episodeId = UUID.randomUUID();
        com.kidsanim.api.dto.EpisodePipelineStatusResponse statusResponse = new com.kidsanim.api.dto.EpisodePipelineStatusResponse(
                episodeId, null, "Episodio Reanudado", "ANIMATION", "ANIMATION", 70, "Reanudando...", null, null, java.time.OffsetDateTime.now(), null
        );
        when(episodePipelineExecutor.resumeEpisode(episodeId)).thenReturn(statusResponse);

        mockMvc.perform(post("/api/v1/episodes/{id}/resume", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.episodeId").value(episodeId.toString()))
                .andExpect(jsonPath("$.stage").value("ANIMATION"));
    }
}
