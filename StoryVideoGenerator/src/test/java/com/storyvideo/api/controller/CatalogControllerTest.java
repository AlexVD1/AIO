package com.storyvideo.api.controller;

import com.storyvideo.api.domain.VisualStyle;
import com.storyvideo.api.infrastructure.security.ApiKeyFilter;
import com.storyvideo.api.repository.VisualStyleRepository;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisualStyleRepository visualStyleRepository;

    @Test
    @DisplayName("GET /api/v1/catalogs/genres debe retornar la lista de géneros disponibles")
    void getGenres_ShouldReturnGenresList() throws Exception {
        mockMvc.perform(get("/api/v1/catalogs/genres")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/catalogs/visual-styles debe retornar la lista de estilos visuales")
    void getVisualStyles_ShouldReturnStyles() throws Exception {
        VisualStyle style = new VisualStyle("Cinematic Dark", "cinematic", "cartoon", "8k", null, true);
        when(visualStyleRepository.findAll()).thenReturn(List.of(style));

        mockMvc.perform(get("/api/v1/catalogs/visual-styles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cinematic Dark"));
    }

    @Test
    @DisplayName("GET /api/v1/catalogs/narrative-arcs debe retornar los arcos narrativos")
    void getNarrativeArcs_ShouldReturnArcs() throws Exception {
        mockMvc.perform(get("/api/v1/catalogs/narrative-arcs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
