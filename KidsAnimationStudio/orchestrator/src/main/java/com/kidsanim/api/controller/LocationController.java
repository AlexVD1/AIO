package com.kidsanim.api.controller;

import com.kidsanim.api.dto.LocationResponse;
import com.kidsanim.api.dto.UpdateLocationRequest;
import com.kidsanim.api.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations", description = "Gestión de locaciones y escenarios de la serie")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una locación por ID")
    public ResponseEntity<LocationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una locación existente")
    public ResponseEntity<LocationResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(locationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una locación")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
