package com.kidsanim.api.controller;

import com.kidsanim.api.domain.enums.VideoModel;
import com.kidsanim.api.dto.CreateStyleProfileRequest;
import com.kidsanim.api.dto.StyleProfileResponse;
import com.kidsanim.api.dto.UpdateStyleProfileRequest;
import com.kidsanim.api.service.StyleProfileService;
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

@WebMvcTest(StyleProfileController.class)
class StyleProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StyleProfileService styleProfileService;

    @Test
    void getAllReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        StyleProfileResponse profile = new StyleProfileResponse(id, "3D Cartoon", "ckpt.safetensors",
                "style", "neg", "dpmpp_sde", 8, new BigDecimal("2.00"), 1024, 576,
                VideoModel.LTX, 16, "768x512", OffsetDateTime.now(), OffsetDateTime.now());

        when(styleProfileService.findAll()).thenReturn(List.of(profile));

        mockMvc.perform(get("/api/v1/style-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("3D Cartoon"))
                .andExpect(jsonPath("$[0].videoModel").value("LTX"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        StyleProfileResponse profile = new StyleProfileResponse(id, "3D Cartoon", "ckpt.safetensors",
                "style", "neg", "dpmpp_sde", 8, new BigDecimal("2.00"), 1024, 576,
                VideoModel.LTX, 16, "768x512", OffsetDateTime.now(), OffsetDateTime.now());

        when(styleProfileService.findById(id)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/style-profiles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("3D Cartoon"));
    }

    @Test
    void createReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        StyleProfileResponse profile = new StyleProfileResponse(id, "3D Cartoon", "ckpt.safetensors",
                "style", "neg", "dpmpp_sde", 8, new BigDecimal("2.00"), 1024, 576,
                VideoModel.LTX, 16, "768x512", OffsetDateTime.now(), OffsetDateTime.now());

        when(styleProfileService.create(any(CreateStyleProfileRequest.class))).thenReturn(profile);

        mockMvc.perform(post("/api/v1/style-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "3D Cartoon",
                                  "checkpointName": "ckpt.safetensors",
                                  "stylePrompt": "style",
                                  "negativePrompt": "neg",
                                  "cfg": 2.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("3D Cartoon"));
    }

    @Test
    void updateReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        StyleProfileResponse profile = new StyleProfileResponse(id, "Updated 3D", "ckpt2.safetensors",
                "style", "neg", "dpmpp_sde", 10, new BigDecimal("2.50"), 1024, 576,
                VideoModel.WAN, 16, "768x512", OffsetDateTime.now(), OffsetDateTime.now());

        when(styleProfileService.update(eq(id), any(UpdateStyleProfileRequest.class))).thenReturn(profile);

        mockMvc.perform(put("/api/v1/style-profiles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated 3D",
                                  "checkpointName": "ckpt2.safetensors",
                                  "stylePrompt": "style",
                                  "negativePrompt": "neg",
                                  "steps": 10,
                                  "cfg": 2.50,
                                  "width": 1024,
                                  "height": 576,
                                  "videoModel": "WAN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated 3D"))
                .andExpect(jsonPath("$.videoModel").value("WAN"));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(styleProfileService).delete(id);

        mockMvc.perform(delete("/api/v1/style-profiles/{id}", id))
                .andExpect(status().isNoContent());
    }
}
