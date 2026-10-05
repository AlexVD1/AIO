package com.kidsanim.api.controller;

import com.kidsanim.api.domain.enums.CharacterRole;
import com.kidsanim.api.domain.enums.CharacterStatus;
import com.kidsanim.api.dto.*;
import com.kidsanim.api.service.CharacterService;
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

@WebMvcTest(CharacterController.class)
class CharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CharacterService characterService;

    @Test
    void getCharacterByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        CharacterResponse response = new CharacterResponse(id, seriesId, "Tito", CharacterRole.HOST,
                "a cute red fox", "curious", "ef_dora", "-12%", "default",
                null, 42L, new BigDecimal("0.85"), CharacterStatus.DRAFT,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(characterService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/characters/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tito"))
                .andExpect(jsonPath("$.role").value("HOST"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void updateCharacterReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        CharacterResponse response = new CharacterResponse(id, seriesId, "Tito el Valiente", CharacterRole.HOST,
                "a brave cute red fox", "brave", "ef_dora", "-10%", "default",
                "/storage/tito.png", 100L, new BigDecimal("0.90"), CharacterStatus.APPROVED,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(characterService.update(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/characters/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tito el Valiente",
                                  "role": "HOST",
                                  "canonicalPrompt": "a brave cute red fox",
                                  "personality": "brave",
                                  "ttsVoice": "ef_dora",
                                  "ttsRate": "-10%",
                                  "ttsPitch": "default",
                                  "referenceImagePath": "/storage/tito.png",
                                  "referenceSeed": 100,
                                  "ipadapterWeight": 0.90,
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tito el Valiente"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void deleteCharacterReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(characterService).delete(id);

        mockMvc.perform(delete("/api/v1/characters/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void generateSheetReturnsSheetResponse() throws Exception {
        UUID id = UUID.randomUUID();
        CharacterSheetResponse sheet = new CharacterSheetResponse(
                id,
                "Tito",
                List.of("/storage/tito_1.png", "/storage/tito_2.png", "/storage/tito_3.png", "/storage/tito_4.png"),
                List.of(42L, 43L, 44L, 45L)
        );

        when(characterService.generateSheet(eq(id), any())).thenReturn(sheet);

        mockMvc.perform(post("/api/v1/characters/{id}/generate-sheet", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "seed": 42,
                                  "count": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterName").value("Tito"))
                .andExpect(jsonPath("$.images.length()").value(4))
                .andExpect(jsonPath("$.seeds[0]").value(42));
    }

    @Test
    void approveCharacterReturnsApprovedCharacter() throws Exception {
        UUID id = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        CharacterResponse approved = new CharacterResponse(id, seriesId, "Tito", CharacterRole.HOST,
                "a cute red fox", "curious", "ef_dora", "-12%", "default",
                "/storage/tito_selected.png", 42L, new BigDecimal("0.85"), CharacterStatus.APPROVED,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(characterService.approve(eq(id), any())).thenReturn(approved);

        mockMvc.perform(post("/api/v1/characters/{id}/approve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imagePath": "/storage/tito_selected.png",
                                  "seed": 42
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tito"))
                .andExpect(jsonPath("$.referenceImagePath").value("/storage/tito_selected.png"))
                .andExpect(jsonPath("$.referenceSeed").value(42))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
