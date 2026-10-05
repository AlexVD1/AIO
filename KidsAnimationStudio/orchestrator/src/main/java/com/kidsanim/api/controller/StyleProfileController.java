package com.kidsanim.api.controller;

import com.kidsanim.api.dto.CreateStyleProfileRequest;
import com.kidsanim.api.dto.StyleProfileResponse;
import com.kidsanim.api.dto.UpdateStyleProfileRequest;
import com.kidsanim.api.service.StyleProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/style-profiles")
@Tag(name = "Style Profiles", description = "Gestión de perfiles de estilo visual y animación")
public class StyleProfileController {

    private final StyleProfileService styleProfileService;

    public StyleProfileController(StyleProfileService styleProfileService) {
        this.styleProfileService = styleProfileService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los perfiles de estilo")
    public ResponseEntity<List<StyleProfileResponse>> getAll() {
        return ResponseEntity.ok(styleProfileService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un perfil de estilo por ID")
    public ResponseEntity<StyleProfileResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(styleProfileService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo perfil de estilo")
    public ResponseEntity<StyleProfileResponse> create(@Valid @RequestBody CreateStyleProfileRequest request) {
        StyleProfileResponse created = styleProfileService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un perfil de estilo existente")
    public ResponseEntity<StyleProfileResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateStyleProfileRequest request) {
        return ResponseEntity.ok(styleProfileService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un perfil de estilo")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        styleProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
