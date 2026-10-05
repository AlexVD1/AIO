package com.kidsanim.api.controller;

import com.kidsanim.api.dto.LocationResponse;
import com.kidsanim.api.dto.UpdateLocationRequest;
import com.kidsanim.api.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @Test
    void getByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        LocationResponse response = new LocationResponse(id, seriesId, "Huerto", "prompt", null,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(locationService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/locations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Huerto"));
    }

    @Test
    void updateReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        LocationResponse response = new LocationResponse(id, seriesId, "Huerto Grande", "prompt mejorado", null,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(locationService.update(eq(id), any(UpdateLocationRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/locations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Huerto Grande",
                                  "prompt": "prompt mejorado"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Huerto Grande"));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(locationService).delete(id);

        mockMvc.perform(delete("/api/v1/locations/{id}", id))
                .andExpect(status().isNoContent());
    }
}
