package com.kidsanim.api.controller;

import com.kidsanim.api.dto.*;
import com.kidsanim.api.service.CharacterService;
import com.kidsanim.api.service.LocationService;
import com.kidsanim.api.service.SeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/series")
@Tag(name = "Series", description = "Gestión de series infantiles y su configuración canónica")
public class SeriesController {

    private final SeriesService seriesService;
    private final CharacterService characterService;
    private final LocationService locationService;

    public SeriesController(SeriesService seriesService,
                            CharacterService characterService,
                            LocationService locationService) {
        this.seriesService = seriesService;
        this.characterService = characterService;
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las series")
    public ResponseEntity<List<SeriesResponse>> getAll() {
        return ResponseEntity.ok(seriesService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle completo de una serie por ID")
    public ResponseEntity<SeriesDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(seriesService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear una nueva serie")
    public ResponseEntity<SeriesResponse> create(@Valid @RequestBody CreateSeriesRequest request) {
        SeriesResponse created = seriesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una serie existente")
    public ResponseEntity<SeriesResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateSeriesRequest request) {
        return ResponseEntity.ok(seriesService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una serie")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        seriesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Endpoints anidados de Personajes ---

    @GetMapping("/{seriesId}/characters")
    @Operation(summary = "Listar personajes de una serie")
    public ResponseEntity<List<CharacterResponse>> getCharacters(@PathVariable UUID seriesId) {
        return ResponseEntity.ok(characterService.findBySeriesId(seriesId));
    }

    @PostMapping("/{seriesId}/characters")
    @Operation(summary = "Crear un personaje en DRAFT dentro de una serie")
    public ResponseEntity<CharacterResponse> createCharacter(@PathVariable UUID seriesId,
                                                             @Valid @RequestBody CreateCharacterRequest request) {
        CharacterResponse created = characterService.create(seriesId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // --- Endpoints anidados de Locaciones ---

    @GetMapping("/{seriesId}/locations")
    @Operation(summary = "Listar locaciones de una serie")
    public ResponseEntity<List<LocationResponse>> getLocations(@PathVariable UUID seriesId) {
        return ResponseEntity.ok(locationService.findBySeriesId(seriesId));
    }

    @PostMapping("/{seriesId}/locations")
    @Operation(summary = "Crear una locación dentro de una serie")
    public ResponseEntity<LocationResponse> createLocation(@PathVariable UUID seriesId,
                                                           @Valid @RequestBody CreateLocationRequest request) {
        LocationResponse created = locationService.create(seriesId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
