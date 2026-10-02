package com.storyvideo.api.controller;

import com.storyvideo.api.domain.NarrativeArc;
import com.storyvideo.api.domain.StoryGenre;
import com.storyvideo.api.domain.StoryTone;
import com.storyvideo.api.domain.VisualStyle;
import com.storyvideo.api.repository.VisualStyleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogs")
@Tag(name = "Catalogs", description = "Catálogos y metadatos disponibles en el sistema")
public class CatalogController {

    private final VisualStyleRepository visualStyleRepository;

    public CatalogController(VisualStyleRepository visualStyleRepository) {
        this.visualStyleRepository = visualStyleRepository;
    }

    @GetMapping("/genres")
    @Operation(summary = "Catálogo de géneros narrativos", description = "Lista todos los géneros admitidos por StoryVideoGenerator")
    public ResponseEntity<List<StoryGenre>> getGenres() {
        return ResponseEntity.ok(Arrays.asList(StoryGenre.values()));
    }

    @GetMapping("/visual-styles")
    @Operation(summary = "Catálogo de estilos visuales", description = "Lista todos los estilos visuales configurados con modificadores para Stable Diffusion")
    public ResponseEntity<List<VisualStyle>> getVisualStyles() {
        return ResponseEntity.ok(visualStyleRepository.findAll());
    }

    @GetMapping("/narrative-arcs")
    @Operation(summary = "Catálogo de arcos narrativos", description = "Lista las estructuras de arco narrativo disponibles")
    public ResponseEntity<List<NarrativeArc>> getNarrativeArcs() {
        return ResponseEntity.ok(Arrays.asList(NarrativeArc.values()));
    }

    @GetMapping("/tones")
    @Operation(summary = "Catálogo de tonos narrativos", description = "Lista los tonos de historia soportados")
    public ResponseEntity<List<StoryTone>> getTones() {
        return ResponseEntity.ok(Arrays.asList(StoryTone.values()));
    }
}
