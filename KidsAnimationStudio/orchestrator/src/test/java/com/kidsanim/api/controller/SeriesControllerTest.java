package com.kidsanim.api.controller;

import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.CharacterStatus;
import com.kidsanim.api.domain.enums.VideoModel;
import com.kidsanim.api.dto.*;
import com.kidsanim.api.service.CharacterService;
import com.kidsanim.api.service.LocationService;
import com.kidsanim.api.service.SeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeriesController.class)
class SeriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeriesService seriesService;

    @MockBean
    private CharacterService characterService;

    @MockBean
    private LocationService locationService;

    @Test
    void getAllSeriesReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID styleId = UUID.randomUUID();
        SeriesResponse response = new SeriesResponse(id, "Tito el zorrito", "Aventuras", "es-MX", 2, 5, "16:9",
                styleId, "playful", OffsetDateTime.now(), OffsetDateTime.now());

        when(seriesService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tito el zorrito"))
                .andExpect(jsonPath("$[0].language").value("es-MX"));
    }

    @Test
    void getSeriesByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID styleId = UUID.randomUUID();
        StyleProfileResponse style = new StyleProfileResponse(styleId, "3D Cartoon", "ckpt.safetensors",
                "3d style", "ugly", "dpmpp_sde", 8, new BigDecimal("2.00"), 1024, 576,
                VideoModel.LTX, 16, "768x512", OffsetDateTime.now(), OffsetDateTime.now());

        SeriesDetailResponse detail = new SeriesDetailResponse(id, "Tito el zorrito", "Desc", "es-MX", 2, 5, "16:9",
                style, "playful", 1, 3, 0, OffsetDateTime.now(), OffsetDateTime.now());

        when(seriesService.findById(id)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/series/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tito el zorrito"))
                .andExpect(jsonPath("$.characterCount").value(1))
                .andExpect(jsonPath("$.locationCount").value(3))
                .andExpect(jsonPath("$.styleProfile.name").value("3D Cartoon"));
    }

    @Test
    void createSeriesReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID styleId = UUID.randomUUID();
        SeriesResponse response = new SeriesResponse(id, "Tito el zorrito", "Desc", "es-MX", 2, 5, "16:9",
                styleId, "playful", OffsetDateTime.now(), OffsetDateTime.now());

        when(seriesService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tito el zorrito",
                                  "description": "Desc",
                                  "styleProfileId": "%s"
                                }
                                """.formatted(styleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tito el zorrito"));
    }

    @Test
    void updateSeriesReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID styleId = UUID.randomUUID();
        SeriesResponse response = new SeriesResponse(id, "Tito y amigos", "Desc nueva", "es-MX", 3, 6, "16:9",
                styleId, "curious", OffsetDateTime.now(), OffsetDateTime.now());

        when(seriesService.update(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tito y amigos",
                                  "description": "Desc nueva",
                                  "language": "es-MX",
                                  "targetAgeMin": 3,
                                  "targetAgeMax": 6,
                                  "aspectRatio": "16:9",
                                  "styleProfileId": "%s",
                                  "defaultBgmMood": "curious"
                                }
                                """.formatted(styleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tito y amigos"));
    }

    @Test
    void deleteSeriesReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(seriesService).delete(id);

        mockMvc.perform(delete("/api/v1/series/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void getCharactersForSeriesReturns200() throws Exception {
        UUID seriesId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();
        CharacterResponse ch = new CharacterResponse(charId, seriesId, "Tito", CharacterRole.HOST,
                "a fox", "friendly", "ef_dora", "-12%", "default", null, 42L,
                new BigDecimal("0.85"), CharacterStatus.DRAFT, OffsetDateTime.now(), OffsetDateTime.now());

        when(characterService.findBySeriesId(seriesId)).thenReturn(List.of(ch));

        mockMvc.perform(get("/api/v1/series/{seriesId}/characters", seriesId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tito"))
                .andExpect(jsonPath("$[0].role").value("HOST"));
    }

    @Test
    void createCharacterForSeriesReturns201() throws Exception {
        UUID seriesId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();
        CharacterResponse ch = new CharacterResponse(charId, seriesId, "Tito", CharacterRole.HOST,
                "a fox", "friendly", "ef_dora", "-12%", "default", null, 42L,
                new BigDecimal("0.85"), CharacterStatus.DRAFT, OffsetDateTime.now(), OffsetDateTime.now());

        when(characterService.create(eq(seriesId), any())).thenReturn(ch);

        mockMvc.perform(post("/api/v1/series/{seriesId}/characters", seriesId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tito",
                                  "canonicalPrompt": "a fox"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tito"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getLocationsForSeriesReturns200() throws Exception {
        UUID seriesId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        LocationResponse loc = new LocationResponse(locId, seriesId, "Huerto", "orchard prompt", null,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(locationService.findBySeriesId(seriesId)).thenReturn(List.of(loc));

        mockMvc.perform(get("/api/v1/series/{seriesId}/locations", seriesId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Huerto"));
    }

    @Test
    void createLocationForSeriesReturns201() throws Exception {
        UUID seriesId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        LocationResponse loc = new LocationResponse(locId, seriesId, "Huerto", "orchard prompt", null,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(locationService.create(eq(seriesId), any())).thenReturn(loc);

        mockMvc.perform(post("/api/v1/series/{seriesId}/locations", seriesId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Huerto",
                                  "prompt": "orchard prompt"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Huerto"));
    }
}
