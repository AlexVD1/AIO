package com.storyvideo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyvideo.api.domain.NarrativeArc;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.domain.StoryTone;
import com.storyvideo.api.dto.StoryGenerationRequestDto;
import com.storyvideo.api.dto.StoryResponseDto;
import com.storyvideo.api.infrastructure.security.ApiKeyFilter;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.StoryRepository;
import com.storyvideo.api.service.StoryGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StoryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class StoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StoryGenerationService storyGenerationService;

    @MockBean
    private com.storyvideo.api.service.StoryVisualService storyVisualService;

    @MockBean
    private com.storyvideo.api.service.StoryAudioService storyAudioService;

    @MockBean
    private com.storyvideo.api.service.VideoRenderService videoRenderService;

    @MockBean
    private StoryRepository storyRepository;

    @MockBean
    private AssetStorageService storageService;

    @MockBean
    private com.storyvideo.api.export.service.ExportService exportService;

    @MockBean
    private com.storyvideo.api.pipeline.service.StoryPipelineExecutor storyPipelineExecutor;

    @Test
    @DisplayName("POST /api/v1/stories/generate con datos válidos debe retornar 201 Created")
    void generateStory_ValidRequest_ShouldReturnCreated() throws Exception {
        StoryGenerationRequestDto request = new StoryGenerationRequestDto(
                StoryGenre.HORROR,
                StoryTone.DARK,
                "Mansión victoriana",
                null,
                90,
                "es-MX",
                4,
                null,
                null
        );

        UUID storyId = UUID.randomUUID();
        StoryResponseDto responseDto = new StoryResponseDto(
                storyId,
                "El Retrato en el Desván",
                StoryGenre.HORROR,
                null,
                StoryTone.DARK,
                "Mansión victoriana",
                "El cuadro cambia cada vez que parpadeas",
                "Un restaurador de arte encuentra un lienzo maldito",
                "Terror gótico",
                NarrativeArc.LINEAR,
                "Él era el modelo original",
                "El rostro en la pintura sonríe",
                "es-MX",
                90,
                StoryStatus.PLANNING_SCENES,
                1,
                "Cinematic Dark",
                List.of(),
                List.of(),
                LocalDateTime.now()
        );

        when(storyGenerationService.generateAndPersistStory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/stories/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(storyId.toString()))
                .andExpect(jsonPath("$.title").value("El Retrato en el Desván"))
                .andExpect(jsonPath("$.genre").value("HORROR"))
                .andExpect(jsonPath("$.status").value("PLANNING_SCENES"));
    }

    @Test
    @DisplayName("POST /api/v1/stories/generate sin género debe retornar 400 Bad Request")
    void generateStory_MissingGenre_ShouldReturnBadRequest() throws Exception {
        String invalidJson = """
                {
                  "theme": "Sin género especificado"
                }
                """;

        mockMvc.perform(post("/api/v1/stories/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.invalidParams.genre").exists());
    }

    @Test
    @DisplayName("GET /api/v1/stories/{id} debe retornar 200 OK cuando existe")
    void getStoryById_Exists_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        StoryResponseDto responseDto = new StoryResponseDto(
                storyId,
                "La Casa Abandonada",
                StoryGenre.MYSTERY,
                null,
                StoryTone.SUSPENSE,
                null,
                "Hook de prueba",
                "Premisa",
                "Sinopsis",
                NarrativeArc.LINEAR,
                null,
                "Desenlace",
                "es-MX",
                60,
                StoryStatus.PLANNING_SCENES,
                1,
                null,
                List.of(),
                List.of(),
                LocalDateTime.now()
        );

        when(storyGenerationService.getStoryById(storyId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/stories/" + storyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storyId.toString()))
                .andExpect(jsonPath("$.title").value("La Casa Abandonada"));
    }

    @Test
    @DisplayName("GET /api/v1/stories/{id} debe retornar 404 Not Found cuando no existe")
    void getStoryById_NotFound_ShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(storyGenerationService.getStoryById(nonExistentId))
                .thenThrow(new NoSuchElementException("Historia no encontrada con ID: " + nonExistentId));

        mockMvc.perform(get("/api/v1/stories/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/generate-images debe retornar 200 OK")
    void generateImages_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        StoryResponseDto responseDto = new StoryResponseDto(
                storyId,
                "La Casa Abandonada",
                StoryGenre.MYSTERY,
                null,
                StoryTone.SUSPENSE,
                null,
                "Hook",
                "Premisa",
                "Sinopsis",
                NarrativeArc.LINEAR,
                null,
                "Desenlace",
                "es-MX",
                60,
                StoryStatus.GENERATING_ASSETS,
                1,
                null,
                List.of(),
                List.of(),
                LocalDateTime.now()
        );

        when(storyVisualService.generateImagesForStory(storyId)).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/generate-images")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storyId.toString()))
                .andExpect(jsonPath("$.status").value("GENERATING_ASSETS"));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/scenes/{sceneId}/regenerate-image debe retornar 200 OK")
    void regenerateSceneImage_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.storyvideo.api.dto.StorySceneResponseDto sceneDto = new com.storyvideo.api.dto.StorySceneResponseDto(
                sceneId,
                1,
                com.storyvideo.api.domain.SceneType.HOOK,
                "Locución",
                "TENSE",
                "Desc",
                "cinematic prompt",
                "stories/" + storyId + "/images/scene_01_v2.png",
                "http://localhost:8081/assets/stories/" + storyId + "/images/scene_01_v2.png",
                com.storyvideo.api.domain.CameraMovement.KEN_BURNS,
                "MEDIUM",
                null,
                com.storyvideo.api.domain.TransitionType.FADE_IN,
                com.storyvideo.api.domain.TransitionType.CUT,
                "STANDARD",
                java.math.BigDecimal.valueOf(5.0),
                java.math.BigDecimal.valueOf(5.0),
                com.storyvideo.api.domain.SceneStatus.IMAGE_READY
        );

        when(storyVisualService.regenerateSceneImage(storyId, sceneId)).thenReturn(sceneDto);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/scenes/" + sceneId + "/regenerate-image")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sceneId.toString()))
                .andExpect(jsonPath("$.status").value("IMAGE_READY"))
                .andExpect(jsonPath("$.imageUrl").value("http://localhost:8081/assets/stories/" + storyId + "/images/scene_01_v2.png"));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/generate-audio debe retornar 200 OK")
    void generateAudio_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        StoryResponseDto responseDto = new StoryResponseDto(
                storyId,
                "Historia de Audio",
                StoryGenre.HORROR,
                null,
                StoryTone.DARK,
                null,
                "Hook",
                "Premisa",
                "Sinopsis",
                NarrativeArc.LINEAR,
                null,
                "Fin",
                "es-MX",
                45,
                StoryStatus.GENERATING_ASSETS,
                1,
                null,
                List.of(),
                List.of(),
                LocalDateTime.now()
        );

        when(storyAudioService.generateAudioForStory(any(UUID.class), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/generate-audio?voice=es-MX-JorgeNeural")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storyId.toString()))
                .andExpect(jsonPath("$.targetDurationSeconds").value(45));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/scenes/{sceneId}/regenerate-audio debe retornar 200 OK")
    void regenerateSceneAudio_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        UUID sceneId = UUID.randomUUID();

        com.storyvideo.api.dto.StorySceneResponseDto sceneDto = new com.storyvideo.api.dto.StorySceneResponseDto(
                sceneId,
                1,
                com.storyvideo.api.domain.SceneType.HOOK,
                "Locución regenerada",
                "TENSE",
                "Desc",
                "prompt",
                null,
                null,
                com.storyvideo.api.domain.CameraMovement.STATIC,
                "HIGH",
                null,
                com.storyvideo.api.domain.TransitionType.CUT,
                com.storyvideo.api.domain.TransitionType.CUT,
                "STANDARD",
                java.math.BigDecimal.valueOf(4.5),
                java.math.BigDecimal.valueOf(5.25),
                com.storyvideo.api.domain.SceneStatus.AUDIO_READY
        );

        when(storyAudioService.regenerateSceneAudio(any(UUID.class), any(UUID.class), any())).thenReturn(sceneDto);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/scenes/" + sceneId + "/regenerate-audio")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sceneId.toString()))
                .andExpect(jsonPath("$.narrationText").value("Locución regenerada"))
                .andExpect(jsonPath("$.status").value("AUDIO_READY"));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/render debe retornar 200 OK con detalles del render")
    void renderVideo_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        UUID renderId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        com.storyvideo.api.dto.VideoRenderResponseDto renderDto = new com.storyvideo.api.dto.VideoRenderResponseDto(
                renderId,
                projectId,
                storyId,
                1,
                "COMPLETED",
                "http://localhost:8081/assets/stories/" + storyId + "/renders/render_v1.mp4",
                12.5,
                "1080x1920",
                "h264",
                true,
                List.of(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(videoRenderService.renderVideo(storyId)).thenReturn(renderDto);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/render")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renderId").value(renderId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resolution").value("1080x1920"))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/stories/{id}/render debe retornar 200 OK con el último render")
    void getLatestRender_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        UUID renderId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        com.storyvideo.api.dto.VideoRenderResponseDto renderDto = new com.storyvideo.api.dto.VideoRenderResponseDto(
                renderId,
                projectId,
                storyId,
                1,
                "COMPLETED",
                "http://localhost:8081/assets/stories/" + storyId + "/renders/render_v1.mp4",
                12.5,
                "1080x1920",
                "h264",
                true,
                List.of(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(videoRenderService.getLatestRender(storyId)).thenReturn(renderDto);

        mockMvc.perform(get("/api/v1/stories/" + storyId + "/render")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renderId").value(renderId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/stories/{id}/video/metadata debe retornar 200 OK con la metadata de exportación")
    void getExportMetadata_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        com.storyvideo.api.export.dto.ExportMetadata meta = new com.storyvideo.api.export.dto.ExportMetadata(
                storyId, "Historia Exportada", "Sinopsis", List.of("#horror"), "HORROR", "Entertainment",
                "es-MX", 45.0, "1080x1920", "mp4", "h264", "aac", 30,
                "2026-10-02T12:00:00", "Historia_Exportada.mp4", 4, true, true, true, "es-MX-JorgeNeural"
        );

        when(exportService.getExportMetadata(storyId)).thenReturn(meta);

        mockMvc.perform(get("/api/v1/stories/" + storyId + "/video/metadata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Historia Exportada"))
                .andExpect(jsonPath("$.genre").value("HORROR"))
                .andExpect(jsonPath("$.has_subtitles").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/export debe retornar 200 OK")
    void exportStory_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        com.storyvideo.api.export.dto.ExportMetadata meta = new com.storyvideo.api.export.dto.ExportMetadata(
                storyId, "Historia Exportada", "Sinopsis", List.of("#horror"), "HORROR", "Entertainment",
                "es-MX", 45.0, "1080x1920", "mp4", "h264", "aac", 30,
                "2026-10-02T12:00:00", "Historia_Exportada.mp4", 4, true, true, true, "es-MX-JorgeNeural"
        );
        com.storyvideo.api.export.dto.ExportResult result = new com.storyvideo.api.export.dto.ExportResult(
                java.nio.file.Paths.get("export/video.mp4"),
                java.nio.file.Paths.get("export/video_metadata.json"),
                meta
        );

        when(exportService.exportStoryVideo(storyId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/export")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.title").value("Historia Exportada"));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/rerender debe retornar 200 OK")
    void rerenderVideo_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        com.storyvideo.api.dto.VideoRenderResponseDto renderDto = new com.storyvideo.api.dto.VideoRenderResponseDto(
                UUID.randomUUID(), UUID.randomUUID(), storyId, 2, "COMPLETED",
                "http://localhost:8081/assets/render.mp4", 45.0, "1080x1920", "h264",
                true, List.of(), null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(videoRenderService.renderVideo(storyId)).thenReturn(renderDto);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/rerender")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.attemptNumber").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{id}/retry debe retornar 200 OK con el estado resultante")
    void retryStory_ShouldReturnOk() throws Exception {
        UUID storyId = UUID.randomUUID();
        com.storyvideo.api.pipeline.dto.PipelineStatusDto status = new com.storyvideo.api.pipeline.dto.PipelineStatusDto(
                storyId, "COMPLETED", com.storyvideo.api.pipeline.dto.PipelineStage.COMPLETED, 100,
                "Pipeline reejecutado exitosamente", "http://localhost:8081/assets/render.mp4",
                "export/video.mp4", List.of(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(storyPipelineExecutor.executeExistingStory(any(), eq(storyId))).thenReturn(status);

        mockMvc.perform(post("/api/v1/stories/" + storyId + "/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercent").value(100));
    }
}
